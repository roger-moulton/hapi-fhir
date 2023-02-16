package ca.uhn.fhir.cr.r4;

import ca.uhn.fhir.cr.BaseCrR4Test;
import ca.uhn.fhir.cr.config.CrProperties;
import ca.uhn.fhir.cr.r4.measure.CareGapsProvider;
import ca.uhn.fhir.cr.r4.measure.CareGapsService;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.fhir.test.utilities.RequestDetailsHelper;
import io.specto.hoverfly.junit5.HoverflyExtension;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.datePart;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

@ExtendWith(SpringExtension.class)
@ExtendWith(HoverflyExtension.class)
class CareGapsProviderIT extends BaseCrR4Test {

	private static final String periodStartValid = "2019-01-01";
	private static IPrimitiveType<Date> periodStart = new DateDt("2019-01-01");
	private static final String periodEndValid = "2019-12-31";
	private static IPrimitiveType<Date> periodEnd = new DateDt("2019-12-31");
	private static final String subjectPatientValid = "Patient/numer-EXM125";
	private static final String subjectGroupValid = "Group/gic-gr-1";
	private static final String subjectGroupParallelValid = "Group/gic-gr-parallel";
	private static final String statusValid = "open-gap";
	private List<String> status;

	private List<CanonicalType> measureUrls;
	private static final String statusValidSecond = "closed-gap";

	private List<String> measures;
	private static final String measureIdValid = "BreastCancerScreeningFHIR";
	private static final String measureUrlValid = "http://ecqi.healthit.gov/ecqms/Measure/BreastCancerScreeningFHIR";
	private static final String practitionerValid = "gic-pra-1";
	private static final String organizationValid = "gic-org-1";
	private static final String dateInvalid = "bad-date";
	private static final String subjectInvalid = "bad-subject";
	private static final String statusInvalid = "bad-status";
	private static final String subjectReferenceInvalid = "Measure/gic-sub-1";

	@Autowired
	Function<RequestDetails, CareGapsService> theCareGapsService;

	@Autowired
	CrProperties crProperties;

	CareGapsProvider theCareGapsProvider;

	@BeforeEach
	void beforeEach() {
		Executor executor = Executors.newSingleThreadExecutor();

		CrProperties.MeasureProperties measureProperties = new CrProperties.MeasureProperties();
		CrProperties.MeasureProperties.MeasureReportConfiguration measureReportConfiguration = new CrProperties.MeasureProperties.MeasureReportConfiguration();
		measureReportConfiguration.setCareGapsReporter("Organization/alphora");
		measureReportConfiguration.setCareGapsCompositionSectionAuthor("Organization/alphora-author");
		measureProperties.setMeasureReport(measureReportConfiguration);
		crProperties.setMeasure(measureProperties);

		theCareGapsProvider = new CareGapsProvider(theCareGapsService);
		readResource("Alphora-organization.json");
		readResource("AlphoraAuthor-organization.json");
		readResource("numer-EXM125-patient.json");
		status = new ArrayList<>();
		measures = new ArrayList<>();
		measureUrls = new ArrayList<>();
	}

	private void beforeEachMeasure() {
		loadBundle("BreastCancerScreeningFHIR-bundle.json");
	}

	private void beforeEachParallelMeasure() {
		readResource("gic-gr-parallel.json");
		loadBundle("BreastCancerScreeningFHIR-bundle.json");
	}

	private void beforeEachMultipleMeasures() {
		loadBundle("BreastCancerScreeningFHIR-bundle.json");
		loadBundle("ColorectalCancerScreeningsFHIR-bundle.json");
	}

	@Test
	void testMinimalParametersValid() {
		beforeEachMeasure();
		periodStart = new DateDt(periodStartValid);
		status.add(statusValid);
		measures.add(measureIdValid);
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setRequestType(RequestTypeEnum.POST);
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", periodEndValid),
			stringPart("subject", subjectPatientValid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid));
		requestDetails.setResource(params);
		var measureReport = theCareGapsProvider.careGapsReport(requestDetails
																							, periodStart
																							, periodEnd
																							, null
																							, subjectPatientValid
																							, null
																							, null
																							, status
																							, measures
																							, null
																							, null
																							, null
																							)	;

		assertNotNull(measureReport);
	}

	@Test
	void testPeriodStartNull() {
		status.add(statusValid);
		measures.add(measureIdValid);
		RequestDetails requestDetails = RequestDetailsHelper.newServletRequestDetails();
		assertThrows(Exception.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, null
			, periodEnd
			, null
			, subjectPatientValid
			, null
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}

	@Test
	void testPeriodStartNullPOST() {
		status.add(statusValid);
		measures.add(measureIdValid);
		RequestDetails requestDetails = RequestDetailsHelper.newServletRequestDetails();
		periodStart = null;
		assertThrows(Exception.class, () ->  theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, subjectPatientValid
			, null
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}

	@Test
	void testPeriodStartInvalid(){
		status.add(statusValid);
		measures.add(measureIdValid);
		RequestDetails requestDetails = RequestDetailsHelper.newServletRequestDetails();
		assertThrows(Exception.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, new DateDt(dateInvalid)
			, periodEnd
			, null
			, subjectPatientValid
			, null
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}

	@Test
	void testPeriodEndNull() {
		status.add(statusValid);
		measures.add(measureIdValid);
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setRequestType(RequestTypeEnum.POST);
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", null),
			stringPart("subject", subjectPatientValid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid));
		requestDetails.setResource(params);
		assertThrows(Exception.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, null
			, null
			, subjectPatientValid
			, null
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}


	@Test
	void testPeriodEndInvalid() {
		try {
			status.add(statusValid);
			measures.add(measureIdValid);
			periodEnd = new DateDt(dateInvalid);
			RequestDetails requestDetails = RequestDetailsHelper.newServletRequestDetails();
			Parameters result = theCareGapsProvider.careGapsReport(requestDetails
				, periodStart
				, periodEnd
				, null
				, subjectPatientValid
				, null
				, null
				, status
				, measures
				, null
				, null
				, null
			);
		} catch (DataFormatException de){
			//do nothing.
		}
	}

	@Test
	void testPeriodInvalid() {
		status.add(statusValid);
		measures.add(measureIdValid);
		RequestDetails requestDetails = RequestDetailsHelper.newServletRequestDetails();
		assertThrows(IllegalArgumentException.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, periodEnd
			, periodStart
			, null
			, subjectPatientValid
			, null
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}


	@Test
	void testSubjectGroupValid() {
		status.add(statusValid);
		measures.add(measureIdValid);
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setRequestType(RequestTypeEnum.POST);
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", periodEndValid),
			stringPart("subject", subjectGroupValid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid));
		requestDetails.setResource(params);
		readResource("gic-gr-1.json");
		assertDoesNotThrow(() -> theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, subjectGroupValid
			, null
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}

	@Test
	void testSubjectInvalid() {
		status.add(statusValid);
		measures.add(measureIdValid);
		periodStart = new DateDt("2019-01-01");
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setRequestType(RequestTypeEnum.POST);
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", periodEndValid),
			stringPart("subject", subjectInvalid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid));
		requestDetails.setResource(params);
		assertThrows(IllegalArgumentException.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, subjectInvalid
			, null
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}

	@Test
	void testSubjectReferenceInvalid() {
		status.add(statusValid);
		measures.add(measureIdValid);
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setRequestType(RequestTypeEnum.POST);
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", periodEndValid),
			stringPart("subject", subjectReferenceInvalid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid));
		requestDetails.setResource(params);
		assertThrows(IllegalArgumentException.class, () ->  theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, subjectReferenceInvalid
			, null
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}

	@Test
	void testSubjectAndPractitioner() {
		status.add(statusValid);
		measures.add(measureIdValid);
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setRequestType(RequestTypeEnum.POST);
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", periodEndValid),
			stringPart("subject", subjectPatientValid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid),
			stringPart("practitioner", practitionerValid));
		requestDetails.setResource(params);
		assertThrows(IllegalArgumentException.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, subjectPatientValid
			, practitionerValid
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}

	@Test
	void testSubjectAndOrganization() {
		status.add(statusValid);
		measures.add(measureIdValid);
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setRequestType(RequestTypeEnum.POST);
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", periodEndValid),
			stringPart("subject", subjectPatientValid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid),
			stringPart("organization", organizationValid));
		requestDetails.setResource(params);
		assertThrows(IllegalArgumentException.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, subjectPatientValid
			, null
			, organizationValid
			, status
			, measures
			, null
			, null
			, null
		));
	}


	@Test
	void testPractitionerAndOrganization() {
		status.add(statusValid);
		measures.add(measureIdValid);
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", periodEndValid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid),
			stringPart("organization", organizationValid),
			stringPart("practitioner", practitionerValid));
		requestDetails.setResource(params);
		Parameters result = theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, null
			, practitionerValid
			, organizationValid
			, status
			, measures
			, null
			, null
			, null
		);

		assertTrue(result.hasParameter("Unsupported configuration"));
	}

	@Test
	void testOrganizationOnly() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid),
				stringPart("organization", organizationValid));
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setResource(params);
		Parameters result = theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, null
			, null
			, organizationValid
			, status
			, measures
			, null
			, null
			, null
		);

		assertTrue(result.hasParameter("Unsupported configuration"));
	}

	@Test
	void testPractitionerOnly() {
		Parameters params = parameters(
			stringPart("periodStart", periodStartValid),
			stringPart("periodEnd", periodEndValid),
			stringPart("status", statusValid),
			stringPart("measureId", measureIdValid),
			stringPart("practitioner", practitionerValid));
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setResource(params);
		assertThrows(IllegalArgumentException.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, periodStart
			, periodEnd
			, null
			, null
			, practitionerValid
			, null
			, status
			, measures
			, null
			, null
			, null
		));
	}

	@Test
	void testSubjectMultiple() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("subject", subjectGroupValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setResource(params);
		assertThrows(Exception.class, () ->  theCareGapsProvider.careGapsReport(requestDetails
			, null, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
		));
	}

	@Test
	void testNoMeasure() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid));
		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setResource(params);
		assertThrows(Exception.class, ()-> theCareGapsProvider.careGapsReport(requestDetails
			, null, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
		));
	}

	@Test
	void testStatusInvalid() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusInvalid),
				stringPart("measureId", measureIdValid));

		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setResource(params);
		assertThrows(IllegalArgumentException.class, ()-> theCareGapsProvider.careGapsReport(requestDetails
			, null, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
		));
	}

	@Test
	void testStatusNull() {
		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("measureId", measureIdValid));

		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setResource(params);
		assertThrows(IllegalArgumentException.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, null, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
		));
	}

	@Test
	void testStatusNullPOST() {
		Parameters params = parameters(
				datePart("periodStart", periodStartValid),
				datePart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("measureId", measureIdValid));

		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setResource(params);
		assertThrows(IllegalArgumentException.class, () -> theCareGapsProvider.careGapsReport(requestDetails
			, null, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
		));
	}

	@Test
	void testMultipleStatusValid() {
		beforeEachMeasure();

		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectPatientValid),
				stringPart("status", statusValid),
				stringPart("status", statusValidSecond),
				stringPart("measureId", measureIdValid));

		assertDoesNotThrow(() -> {
			SystemRequestDetails requestDetails = new SystemRequestDetails();
			requestDetails.setRequestType(RequestTypeEnum.POST);
			requestDetails.setFhirContext(getFhirContext());
			requestDetails.setResource(params);
			Parameters result = theCareGapsProvider.careGapsReport(requestDetails
				, null, null
				, null
				, null
				, null
				, null
				, null
				, null
				, null
				, null
				, null
			);
		});
	}

	@Test
	void testMeasures() {
		beforeEachMultipleMeasures();
		status.add(statusValid);
		periodStart = new DateDt("2019-01-01");
		measures.add("ColorectalCancerScreeningsFHIR");
		measureUrls.add(new CanonicalType(measureUrlValid));

		SystemRequestDetails requestDetails = new SystemRequestDetails();
		//requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setFhirServerBase("test.com");
		//requestDetails.setResource(params);
		Parameters result = theCareGapsService.apply(requestDetails).getCareGapsReport(periodStart, periodEnd
			, null
			, subjectPatientValid
			, null
			, null
			, status
			, measures
			, null
			, measureUrls
			, null
		);

		assertNotNull(result);
	}

	@Test
	void testParallelMultiSubject() {
		beforeEachParallelMeasure();

		Parameters params = parameters(
				stringPart("periodStart", periodStartValid),
				stringPart("periodEnd", periodEndValid),
				stringPart("subject", subjectGroupParallelValid),
				stringPart("status", statusValid),
				stringPart("measureId", measureIdValid));

		SystemRequestDetails requestDetails = new SystemRequestDetails();
		requestDetails.setRequestType(RequestTypeEnum.POST);
		requestDetails.setFhirContext(getFhirContext());
		requestDetails.setResource(params);
		Parameters result = theCareGapsProvider.careGapsReport(requestDetails
			, null, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
			, null
		);

		assertNotNull(result);
	}
}
