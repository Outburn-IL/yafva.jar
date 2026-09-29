package il.co.outburn.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


@OpenAPIDefinition(info = @Info(title = "FHIR REST API validator"))
@RestController
@Slf4j
public class FhirValidationController {
    private static final String CONTENT_TYPE_APPLICATION_FHIR_JSON = "application/fhir+json";
    private static final String CONTENT_TYPE_APPLICATION_FHIR_JSON_UTF8 = "application/fhir+json;charset=UTF-8";
    private static final String CONTENT_TYPE_APPLICATION_FHIR_XML = "application/fhir+xml";
    private static final String CONTENT_TYPE_APPLICATION_FHIR_XML_UTF8 = "application/fhir+xml;charset=UTF-8";

    FhirValidationController() {
        log.info("FhirValidationController constructor called");
    }

    @Autowired
    FhirValidatorConfiguration configuration;

    @Hidden
    @GetMapping(value = "/", produces = {MediaType.TEXT_HTML_VALUE})
    public String index() {
        String txServer = configuration.getTxServer();
        if (txServer == null) txServer = "";

        var fhirVersion = configuration.sv;
        if (fhirVersion == null)
            fhirVersion = "4.0.1";
        var result = new StringBuilder();
        result.append("<html><head><title>YAFVA.JAR REST API</title>" +
                "<link rel='stylesheet' href='/css/bootstrap.min.css'>" +
                "</head>");
        result.append("<body><div class='container'><h1 class='h1'>Outburn FHIR REST API validator</h1>");
        result.append("<div class='mt-2 mb-2'><strong>Configuration</strong></div>");
        result.append("<table class='table'>");
        result.append("<tr><td>FHIR version</td><td>").append(fhirVersion).append("</td></tr>");
        result.append("<tr><td>Used implementation guides</td><td>").append(String.join("<br />", configuration.ig)).append("</td></tr>");
        result.append("<tr><td>Terminology server URL</td><td>").append(txServer).append("</td></tr>");
        result.append("</table>");
        result.append("<div class='mt-2 mb-2'><a href='/swagger-ui/index.html'>Swagger / OpenAPI</a></div>");
        result.append("</div></body></html>");
        return result.toString();
    }

    @Operation(
            summary = "Validates a FHIR resource",
            requestBody = @RequestBody(
                description = "A FHIR resource to validate, in JSON or XML format",
                content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(type = "object")),
                    @Content(mediaType = CONTENT_TYPE_APPLICATION_FHIR_JSON, schema = @Schema(type = "object")),
                    @Content(mediaType = CONTENT_TYPE_APPLICATION_FHIR_XML, schema = @Schema(type = "string"))
                },
                required = true),
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Success",
                        content = {
                            @Content(mediaType = CONTENT_TYPE_APPLICATION_FHIR_JSON_UTF8, schema = @Schema(type = "object")),
                            @Content(mediaType = CONTENT_TYPE_APPLICATION_FHIR_XML_UTF8, schema = @Schema(type = "string"))
                        }),
                    @ApiResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
    @Parameter(
            in = ParameterIn.QUERY,
            name = "profile",
            required = false,
            array = @ArraySchema(schema = @Schema(type = "string")), description = "Optional. A list of FHIR profile URLs")
    @Parameter(
            in = ParameterIn.QUERY,
            name = "format",
            required = false,
            schema = @Schema(type = "string"), description = "Optional. Response format. Specify 'list' to return a list of messages, or specify 'outcome' to return validation results as an instance of FHIR OperationOutcome resource. Default value is 'outcome'.")
    @PostMapping(
            value = "/validate",
            consumes = {MediaType.APPLICATION_JSON_VALUE, "text/json", CONTENT_TYPE_APPLICATION_FHIR_JSON, MediaType.APPLICATION_XML_VALUE, "text/xml", CONTENT_TYPE_APPLICATION_FHIR_XML},
            produces = {MediaType.APPLICATION_JSON_VALUE, CONTENT_TYPE_APPLICATION_FHIR_JSON, CONTENT_TYPE_APPLICATION_FHIR_XML, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<?> validateRequest(
            HttpServletRequest request,
            @RequestParam(value = "profile", required = false) List<String> profiles,
            @RequestParam(value = "format", required = false) String format) throws Throwable {
        try {
            if (format == null || format.isEmpty()) {
                format = "outcome";
            }

            log.info("FhirValidationController::validate called");
            var body = request.getInputStream();
            var bytes = body.readAllBytes();
            var contentType = request.getContentType();
            var result = FhirValidator.validateBytes(bytes, profiles, contentType, configuration);
            if ("outcome".equals(format)) {
                // Determine response content type based on request content type
                MediaType responseContentType;
                if (FhirValidator.isXmlFormat(contentType)) {
                    responseContentType = MediaType.parseMediaType(CONTENT_TYPE_APPLICATION_FHIR_XML_UTF8);
                } else {
                    responseContentType = MediaType.parseMediaType(CONTENT_TYPE_APPLICATION_FHIR_JSON_UTF8);
                }
                return ResponseEntity
                        .ok()
                        .contentType(responseContentType)
                        .body(result.resourceBytes);
            } else {
                var response = new FhirValidatorResponse();
                response.messages = result.messages;
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            }
        } catch (IllegalArgumentException ex) {
            log.error("FhirValidationController::validate - bad request: ", ex);
            var pd = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), ex.getMessage());
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON_UTF8)
                    .body(pd);
        } catch (Exception ex) {
            log.error("FhirValidationController::validate - internal server error: ", ex);
            var pd = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500), ex.getMessage());
            return ResponseEntity
                    .internalServerError()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON_UTF8)
                    .body(pd);
        }
    }

    @Operation(
            summary = "Performs validation of the input FHIR Bundle in batch mode and returns a response FHIR Bundle that contains OperationOutcome for each resource",
            requestBody = @RequestBody(
                description = "A FHIR bundle to validate, in JSON or XML format",
                content = {
                    @Content(mediaType = CONTENT_TYPE_APPLICATION_FHIR_JSON, schema = @Schema(type = "object")),
                    @Content(mediaType = CONTENT_TYPE_APPLICATION_FHIR_XML, schema = @Schema(type = "string"))
                },
                required = true),
                responses = {
                    @ApiResponse(
                        responseCode = "200",
                        description = "Success. Returned value is a FHIR Bundle of type collection.",
                        content = {
                            @Content(mediaType = CONTENT_TYPE_APPLICATION_FHIR_JSON_UTF8, schema = @Schema(type = "object")),
                            @Content(mediaType = CONTENT_TYPE_APPLICATION_FHIR_XML_UTF8, schema = @Schema(type = "string"))
                        }),
                    @ApiResponse(
                        responseCode = "500",
                        description = "Internal Server Error",
                        content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
    @PostMapping(
            value = "/validateBundle",
            consumes = {MediaType.APPLICATION_JSON_VALUE, "text/json", CONTENT_TYPE_APPLICATION_FHIR_JSON, MediaType.APPLICATION_XML_VALUE, "text/xml", CONTENT_TYPE_APPLICATION_FHIR_XML},
            produces = {CONTENT_TYPE_APPLICATION_FHIR_JSON, CONTENT_TYPE_APPLICATION_FHIR_XML, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<?> validateBundle(HttpServletRequest request) throws Throwable {
        try {
            log.info("FhirValidationController::validateBundle called");
            var body = request.getInputStream();
            var bytes = body.readAllBytes();
            var contentType = request.getContentType();
            var result = FhirValidator.validateBundle(bytes, contentType, configuration);
            // Determine response content type based on request content type
            MediaType responseContentType;
            if (FhirValidator.isXmlFormat(contentType)) {
                responseContentType = MediaType.parseMediaType(CONTENT_TYPE_APPLICATION_FHIR_XML_UTF8);
            } else {
                responseContentType = MediaType.parseMediaType(CONTENT_TYPE_APPLICATION_FHIR_JSON_UTF8);
            }
            return ResponseEntity
                    .ok()
                    .contentType(responseContentType)
                    .body(result.resourceBytes);

        } catch (IllegalArgumentException ex) {
            log.error("FhirValidationController::validateBundle - bad request: ", ex);
            var pd = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), ex.getMessage());
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON_UTF8)
                    .body(pd);
        } catch (Exception ex) {
            log.error("FhirValidationController::validateBundle - internal server error: ", ex);
            var pd = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500), ex.getMessage());
            return ResponseEntity
                    .internalServerError()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON_UTF8)
                    .body(pd);
        }
    }

    @Operation(
            summary = "Get application configuration and environment information",
            responses = {
                @ApiResponse(
                    responseCode = "200",
                    description = "Success",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApplicationProperties.ApplicationInfo.class))),
                @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))})
    @GetMapping(value = "/info", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_PROBLEM_JSON_VALUE})
    public ResponseEntity<?> getInfo() {
        try {
            log.info("FhirValidationController::getInfo called");
            return ResponseEntity.ok(new ApplicationProperties.ApplicationInfo(configuration));
        } catch (Exception ex) {
            log.error("FhirValidationController::getInfo - internal server error: ", ex);
            var pd = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500), ex.getMessage());
            return ResponseEntity
                    .internalServerError()
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(pd);
        }
    }
}
