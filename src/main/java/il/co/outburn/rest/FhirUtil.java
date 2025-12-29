package il.co.outburn.rest;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.fhirpath.FHIRPathEngine;
import org.hl7.fhir.r4.formats.IParser;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.utils.OperationOutcomeUtilities;
import org.hl7.fhir.r4.utils.ToolingExtensions;
import org.hl7.fhir.utilities.i18n.I18nConstants;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FhirUtil {
    @Getter
    private static final JsonParser parserR4;

    @Getter
    private static final org.hl7.fhir.r5.formats.JsonParser parserR5;

    @Getter
    private static final org.hl7.fhir.r4.formats.XmlParser xmlParserR4;

    @Getter
    private static final org.hl7.fhir.r5.formats.XmlParser xmlParserR5;

    static {
        parserR4 = new JsonParser();
        parserR4.setOutputStyle(IParser.OutputStyle.NORMAL);
        parserR5 = new org.hl7.fhir.r5.formats.JsonParser();
        parserR5.setOutputStyle(org.hl7.fhir.r5.formats.IParser.OutputStyle.NORMAL);
        xmlParserR4 = new org.hl7.fhir.r4.formats.XmlParser();
        xmlParserR4.setOutputStyle(IParser.OutputStyle.NORMAL);
        xmlParserR5 = new org.hl7.fhir.r5.formats.XmlParser();
        xmlParserR5.setOutputStyle(org.hl7.fhir.r5.formats.IParser.OutputStyle.NORMAL);
    }

    private static List<ValidationMessage> filterMessages(List<ValidationMessage> messages) {
        var filteredValidation = new ArrayList<ValidationMessage>();
        for (var e : messages) {
            if (!filteredValidation.contains(e))
                filteredValidation.add(e);
        }
        filteredValidation.sort(null);
        return filteredValidation;
    }

    public static OperationOutcome messagesToOutcome(List<ValidationMessage> messages, SimpleWorkerContext context, FHIRPathEngine fpe) {
        OperationOutcome op = new OperationOutcome();
        for (ValidationMessage vm : filterMessages(messages)) {
            try {
                fpe.parse(vm.getLocation());
            } catch (Exception e) {
                System.out.println("Internal error in location for message: '" + e.getMessage() + "', loc = '" + vm.getLocation() + "', err = '" + vm.getMessage() + "'");
            }
            op.getIssue().add(OperationOutcomeUtilities.convertToIssue(vm, op));
        }
        if (!op.hasIssue()) {
            op.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION).setCode(OperationOutcome.IssueType.INFORMATIONAL).getDetails().setText(context.formatMessage(I18nConstants.ALL_OK));
        }
        //RenderingContext rc = new RenderingContext(context, null, null, "http://hl7.org/fhir", "", null, RenderingContext.ResourceRendererMode.END_USER, RenderingContext.GenerationRules.VALID_RESOURCE);
        //RendererFactory.factory(op, rc).renderResource(ResourceWrapper.forResource(rc.getContextUtilities(), op));
        return op;
    }

    public static org.hl7.fhir.r5.model.OperationOutcome exceptionToOutcome(Throwable e) {
        return org.hl7.fhir.r5.utils.OperationOutcomeUtilities.outcomeFromTextError(e.toString());
    }

    public static boolean operationOutcomeHasErrorIssue(OperationOutcome outcome) {
        if (!outcome.hasIssue()) {
        return false;
        }
        
        for (var item : outcome.getIssue()) {
        if (!item.isEmpty() && item.hasSeverity() &&
            (item.getSeverity() == OperationOutcome.IssueSeverity.FATAL || item.getSeverity() == OperationOutcome.IssueSeverity.ERROR)) {
            return true;
        }
        }
        return false;
    }

    private static String getString(OperationOutcome resource) {
        int error = 0;
        int warn = 0;
        int info = 0;
        for (OperationOutcome.OperationOutcomeIssueComponent issue : resource.getIssue()) {
            if (issue.getSeverity() == OperationOutcome.IssueSeverity.FATAL || issue.getSeverity() == OperationOutcome.IssueSeverity.ERROR)
                error++;
            else if (issue.getSeverity() == OperationOutcome.IssueSeverity.WARNING)
                warn++;
            else
                info++;
        }
        return ((error == 0 ? "Success" : "*FAILURE*") + ": " + error + " errors, " + warn + " warnings, " + info + " notes");
    }

    public static String getIssueSummary(OperationOutcome.OperationOutcomeIssueComponent issue) {
        String loc;
        if (issue == null) return "";
        if (issue.hasExpression()) {
            int line = ToolingExtensions.readIntegerExtension(issue, ToolingExtensions.EXT_ISSUE_LINE, -1);
            int col = ToolingExtensions.readIntegerExtension(issue, ToolingExtensions.EXT_ISSUE_COL, -1);
            loc = issue.getExpression().get(0).asStringValue() + (line >= 0 && col >= 0 ? " (line " + line + ", col" + col + ")" : "");
        } else if (issue.hasLocation()) {
            loc = issue.getLocation().get(0).asStringValue();
        } else {
            int line = ToolingExtensions.readIntegerExtension(issue, ToolingExtensions.EXT_ISSUE_LINE, -1);
            int col = ToolingExtensions.readIntegerExtension(issue, ToolingExtensions.EXT_ISSUE_COL, -1);
            loc = (line >= 0 && col >= 0 ? "line " + line + ", col" + col : "??");
        }
        return "  " + issue.getSeverity().getDisplay() + " @ " + loc + " : " + issue.getDetails().getText();
    }

    public static Object bytesToResource(byte[] bytes, FhirValidatorConfiguration configuration) throws Exception {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Input bytes cannot be null or empty");
        }
        return configuration.isR4Ver() ? parserR4.parse(bytes) : parserR5.parse(bytes);
    }

    public static List<String> split(String value) {
        var result = new ArrayList<String>();
        if (value == null || value.trim().isEmpty()) return result;

        var arr = value.split("[,;\\s]");
        for (String item : arr) {
            if (item != null && !item.trim().isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    public static String getResourceFile(String resourceName) {
        var classLoader = FhirUtil.class.getClassLoader();
        var resource = classLoader.getResource(resourceName);
        assert resource != null;
        var file = new File(resource.getFile());
        return file.getAbsolutePath();
    }

    public static InputStream getResourceStream(String resourceName) {
        return FhirUtil.class.getResourceAsStream(resourceName);
    }

    public static boolean StringEquals(String str1, String str2) {
        if (str1 == null) str1 = "";
        if (str2 == null) str2 = "";
        return str1.equals(str2);
    }
}
