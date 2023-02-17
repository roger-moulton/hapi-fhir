package ca.uhn.fhir.cr.r4;

import ca.uhn.fhir.cr.BaseCrR4Test;
import ca.uhn.fhir.cr.config.CrProperties;
import ca.uhn.fhir.cr.r4.measure.CareGapsOperationProvider;
import ca.uhn.fhir.cr.r4.measure.CareGapsService;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.parameters;
import static org.opencds.cqf.cql.evaluator.fhir.util.r4.Parameters.stringPart;

@ExtendWith(SpringExtension.class)
@ExtendWith(HoverflyExtension.class)
class CareGapsOperationProviderIT extends BaseCrR4Test {

	private static final String periodStartValid = "2019-01-01";
	private static IPrimitiveType<Date> periodStart = new DateDt("2019-01-01");
	private static final String periodEndValid = "2019-12-31";
	private static IPrimitiveType<Date> periodEnd = new DateDt("2019-12-31");
	private static final String subjectPatientValid = "Patient/numer-EXM125";
	private static final String statusValid = "open-gap";
	private List<String> status;

	private List<CanonicalType> measureUrls;

	private List<String> measures;
	private static final String measureIdValid = "BreastCancerScreeningFHIR";

	@Autowired
	Function<RequestDetails, CareGapsService> theCareGapsService;

	@Autowired
	CrProperties crProperties;

	CareGapsOperationProvider theCareGapsProvider;

	@BeforeEach
	void beforeEach() {
		Executor executor = Executors.newSingleThreadExecutor();

		CrProperties.MeasureProperties measureProperties = new CrProperties.MeasureProperties();
		CrProperties.MeasureProperties.MeasureReportConfiguration measureReportConfiguration = new CrProperties.MeasureProperties.MeasureReportConfiguration();
		measureReportConfiguration.setCareGapsReporter("Organization/alphora");
		measureReportConfiguration.setCareGapsCompositionSectionAuthor("Organization/alphora-author");
		measureProperties.setMeasureReport(measureReportConfiguration);
		crProperties.setMeasure(measureProperties);

		theCareGapsProvider = new CareGapsOperationProvider(theCareGapsService);
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
	public void operationProviderWasRegister(){
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
		assertNotNull(theCareGapsProvider.careGapsReport(requestDetails
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
		))	;
	}

}
