package ca.uhn.fhir.jpa.term;

import ca.uhn.fhir.jpa.entity.TermCodeSystemVersion;
import ca.uhn.fhir.jpa.entity.TermConcept;
import ca.uhn.fhir.jpa.entity.TermConceptParentChildLink;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.TestUtil;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.ConceptMap;
import org.hl7.fhir.r4.model.ValueSet;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.*;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TerminologyLoaderSvcSnomedCtTest {
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TerminologyLoaderSvcSnomedCtTest.class);
	private TerminologyLoaderSvcImpl mySvc;

	@Mock
	private IHapiTerminologySvc myTermSvc;
	@Captor
	private ArgumentCaptor<TermCodeSystemVersion> myCsvCaptor;
	@Mock
	private IHapiTerminologySvcDstu3 myTermSvcDstu3;
	private ZipCollectionBuilder myFiles;

	@Before
	public void before() {
		mySvc = new TerminologyLoaderSvcImpl();
		mySvc.setTermSvcForUnitTests(myTermSvc);
		mySvc.setTermSvcDstu3ForUnitTest(myTermSvcDstu3);

		myFiles = new ZipCollectionBuilder();
	}

	private List<byte[]> list(byte[]... theByteArray) {
		return new ArrayList<>(Arrays.asList(theByteArray));
	}

	@Test
	public void testLoadSnomedCt() throws Exception {
		myFiles.addFile("/sct/", "sct2_Concept_Full_INT_20160131.txt");
		myFiles.addFile("/sct/", "sct2_Concept_Full-en_INT_20160131.txt");
		myFiles.addFile("/sct/", "sct2_Description_Full-en_INT_20160131.txt");
		myFiles.addFile("/sct/", "sct2_Identifier_Full_INT_20160131.txt");
		myFiles.addFile("/sct/", "sct2_Relationship_Full_INT_20160131.txt");
		myFiles.addFile("/sct/", "sct2_StatedRelationship_Full_INT_20160131.txt");
		myFiles.addFile("/sct/", "sct2_TextDefinition_Full-en_INT_20160131.txt");

		RequestDetails details = mock(RequestDetails.class);
		mySvc.loadSnomedCt(myFiles.getFiles(), details);

		verify(myTermSvcDstu3).storeNewCodeSystemVersion(any(CodeSystem.class), myCsvCaptor.capture(), any(RequestDetails.class), anyListOf(ValueSet.class), anyListOf(ConceptMap.class));

		TermCodeSystemVersion csv = myCsvCaptor.getValue();
		TreeSet<String> allCodes = toCodes(csv, true);
		ourLog.info(allCodes.toString());

		assertThat(allCodes, containsInRelativeOrder("116680003"));
		assertThat(allCodes, not(containsInRelativeOrder("207527008")));

		allCodes = toCodes(csv, false);
		ourLog.info(allCodes.toString());
		assertThat(allCodes, hasItem("126816002"));
	}

	/**
	 * This is just for trying stuff, it won't run without
	 * local files external to the git repo
	 */
	@Ignore
	@Test
	public void testLoadSnomedCtAgainstRealFile() throws Exception {
		byte[] bytes = IOUtils.toByteArray(new FileInputStream("/Users/james/Downloads/SnomedCT_Release_INT_20160131_Full.zip"));

		RequestDetails details = mock(RequestDetails.class);
		mySvc.loadSnomedCt(list(bytes), details);
	}

	@Test
	public void testLoadSnomedCtBadInput() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(bos);
		myFiles.addFile("/sct/", "sct2_StatedRelationship_Full_INT_20160131.txt");
		zos.close();

		ourLog.info("ZIP file has {} bytes", bos.toByteArray().length);

		RequestDetails details = mock(RequestDetails.class);
		try {
			mySvc.loadSnomedCt(Collections.singletonList(bos.toByteArray()), details);
			fail();
		} catch (InvalidRequestException e) {
			assertEquals("Invalid input zip file, expected zip to contain the following name fragments: [Terminology/sct2_Description_Full-en, Terminology/sct2_Relationship_Full, Terminology/sct2_Concept_Full_] but found: []", e.getMessage());
		}
	}

	private TreeSet<String> toCodes(TermCodeSystemVersion theCsv, boolean theAddChildren) {
		TreeSet<String> retVal = new TreeSet<>();
		for (TermConcept next : theCsv.getConcepts()) {
			toCodes(retVal, next, theAddChildren);
		}
		return retVal;
	}

	private void toCodes(TreeSet<String> theCodes, TermConcept theConcept, boolean theAddChildren) {
		theCodes.add(theConcept.getCode());
		if (theAddChildren) {
			for (TermConceptParentChildLink next : theConcept.getChildren()) {
				toCodes(theCodes, next.getChild(), theAddChildren);
			}
		}
	}

	@AfterClass
	public static void afterClassClearContext() {
		TestUtil.clearAllStaticFieldsForUnitTest();
	}

}
