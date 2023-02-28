package ca.uhn.fhir.jpa.dao.r4;

import ca.uhn.fhir.interceptor.api.HookParams;
import ca.uhn.fhir.interceptor.api.IPointcut;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.model.DaoMethodOutcome;
import ca.uhn.fhir.jpa.api.svc.IResourceSearchUrlSvc;
import ca.uhn.fhir.jpa.dao.data.IResourceSearchUrlDao;
import ca.uhn.fhir.jpa.model.entity.ResourceSearchUrlEntity;
import ca.uhn.fhir.jpa.search.SearchUrlJobMaintenanceSvcImpl;
import ca.uhn.fhir.jpa.test.BaseJpaR4Test;
import ca.uhn.fhir.jpa.test.config.TestR4Config;
import ca.uhn.fhir.rest.api.server.SystemRequestDetails;
import ca.uhn.test.concurrency.PointcutLatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Observation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class FhirResourceDaoR4ConcurrentCreateTest extends BaseJpaR4Test {

	private static final Logger ourLog = LoggerFactory.getLogger(FhirResourceDaoR4ConcurrentCreateTest.class);

	ThreadGaterPointcutLatch myThreadGaterPointcutLatch;
	ResourceConcurrentSubmitterSvc myResourceConcurrentSubmitterSvc;

	@Autowired
	SearchUrlJobMaintenanceSvcImpl mySearchUrlJobMaintenanceSvc;

	@Autowired
	IResourceSearchUrlDao myResourceSearchUrlDao;

	@Autowired
	IResourceSearchUrlSvc myResourceSearchUrlSvc;

	Callable<String> myResource;

	@BeforeEach
	public void beforeEach(){
		myThreadGaterPointcutLatch = new ThreadGaterPointcutLatch("gaterLatch");
		myInterceptorRegistry.registerAnonymousInterceptor(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED, myThreadGaterPointcutLatch);
		myResourceConcurrentSubmitterSvc = new ResourceConcurrentSubmitterSvc();
		myResource = getResource();
	}

	@AfterEach
	public void afterEach() {
		myResourceConcurrentSubmitterSvc.shutDown();
	}

	@Override
	@AfterEach
	public void afterResetInterceptors() {
		super.afterResetInterceptors();
		myInterceptorRegistry.unregisterInterceptor(myThreadGaterPointcutLatch);
	}

	@Test
	public void testMultipleThreads_attemptingToCreatingTheSameResource_willCreateOnlyOneResource() throws InterruptedException, ExecutionException {

		final int numberOfThreadsAttemptingToCreateDuplicates = 2;
		int expectedResourceCount = myResourceTableDao.findAll().size() + 1;

		myThreadGaterPointcutLatch.setExpectedCount(numberOfThreadsAttemptingToCreateDuplicates);

		// create a situation where multiple threads will try to create the same resource;
		for (int i = 0; i < numberOfThreadsAttemptingToCreateDuplicates; i++){
			myResourceConcurrentSubmitterSvc.submitResource(myResource);
		}

		// let's wait for all executor threads to wait (block) at the starting line
		ourLog.info("awaitExpected");
		myThreadGaterPointcutLatch.awaitExpected();

		ourLog.info("waking up the sleepers");
		myThreadGaterPointcutLatch.doNotifyAll();
		
		List<String> errorList = myResourceConcurrentSubmitterSvc.waitForThreadsCompletionAndReturnErrors();

		// then
		assertThat(errorList, hasSize(0));
		// red-green before the fix, the size was 'numberOfThreadsAttemptingToCreateDuplicates'
		assertThat(myResourceTableDao.findAll(), hasSize(expectedResourceCount));

	}

	@Test
	public void testRemoveStaleEntries_withNonStaleAndStaleEntries_willOnlyDeleteStaleEntries(){
		// given
		long tenMinutes = 10 * DateUtils.MILLIS_PER_HOUR;

		Date tooOldBy10Minutes = cutOffTimeMinus(tenMinutes);
		ResourceSearchUrlEntity tooOld1 = ResourceSearchUrlEntity.from("Observation?identifier=20210427133226.444", 1l).setCreatedTime(tooOldBy10Minutes);
		ResourceSearchUrlEntity tooOld2 = ResourceSearchUrlEntity.from("Observation?identifier=20210427133226.445", 2l).setCreatedTime(tooOldBy10Minutes);

		Date tooNewBy10Minutes = cutOffTimePlus(tenMinutes);
		ResourceSearchUrlEntity tooNew1 = ResourceSearchUrlEntity.from("Observation?identifier=20210427133226.446", 3l).setCreatedTime(tooNewBy10Minutes);
		ResourceSearchUrlEntity tooNew2 =ResourceSearchUrlEntity.from("Observation?identifier=20210427133226.447", 4l).setCreatedTime(tooNewBy10Minutes);

		myResourceSearchUrlDao.saveAll(asList(tooOld1, tooOld2, tooNew1, tooNew2));

		// when
		mySearchUrlJobMaintenanceSvc.removeStaleEntries();

		// then
		List<ResourceSearchUrlEntity> remainingSearchUrlEntities = myResourceSearchUrlDao.findAll();
		List<Long> resourcesPids = remainingSearchUrlEntities.stream().map(ResourceSearchUrlEntity::getResourcePid).collect(Collectors.toList());
		assertThat(resourcesPids, containsInAnyOrder(3l, 4l));
	}

	@Test
	public void testRemoveStaleEntries_withNoEntries_willNotGenerateExceptions(){
		List<ResourceSearchUrlEntity> all = myResourceSearchUrlDao.findAll();
		assertThat(all, hasSize(0));

		mySearchUrlJobMaintenanceSvc.removeStaleEntries();

	}

	private Date cutOffTimePlus(long theAdjustment) {
		long currentTimeMillis = System.currentTimeMillis();
		long offset = currentTimeMillis - SearchUrlJobMaintenanceSvcImpl.OUR_CUTOFF_IN_MILLISECONDS + theAdjustment;
		return new Date(offset);
	}

	private Date cutOffTimeMinus(long theAdjustment) {
		return cutOffTimePlus(-theAdjustment);
	}

	private Callable<String> getResource() {
		return () -> {

			Identifier identifier = new Identifier().setValue("20210427133226.444+0800");
			Observation obs = new Observation().addIdentifier(identifier);

			try {
				ourLog.info("Creating resource");
				DaoMethodOutcome outcome = myObservationDao.create(obs, "identifier=20210427133226.444+0800", new SystemRequestDetails());
			} catch (Throwable t) {
				ourLog.info("create threw an exception {}", t.getMessage());
			}
			return null;
		};
		
	}

	public static class ThreadGaterPointcutLatch extends PointcutLatch {
		public ThreadGaterPointcutLatch(String theName) {
			super(theName);
		}

		public void invoke(IPointcut thePointcut, HookParams theArgs)  {
			doInvoke(thePointcut, theArgs);
		}

		private synchronized void doInvoke(IPointcut thePointcut, HookParams theArgs){
			super.invoke(thePointcut, theArgs);
			try {
				String threadName = Thread.currentThread().getName();
				ourLog.info(String.format("I'm thread %s and i'll going to sleep", threadName));
				wait(10*1000);
				ourLog.info(String.format("I'm thread %s and i'm waking up", threadName));
			} catch (InterruptedException theE) {
				throw new RuntimeException(theE);
			}
		}

		public synchronized void doNotifyAll(){
			notifyAll();
		}

	}

	public static class ResourceConcurrentSubmitterSvc{
		ExecutorService myPool;
		List<Future<String>> myFutures = new ArrayList<>();
		public List<String> waitForThreadsCompletionAndReturnErrors() throws ExecutionException, InterruptedException {

			List<String> errorList = new ArrayList<>();

			for (Future<String> next : myFutures) {
				String nextError = next.get();
				if (StringUtils.isNotBlank(nextError)) {
					errorList.add(nextError);
				}
			}
			return errorList;
		}

		private ExecutorService getExecutorServicePool(){
			if(Objects.isNull(myPool)){
				int maxThreadsUsed = TestR4Config.ourMaxThreads - 1;
				myPool = Executors.newFixedThreadPool(Math.min(maxThreadsUsed, 5));
			}

			return myPool;
		}

		public void shutDown(){
			getExecutorServicePool().shutdown();
		}

		public void submitResource(Callable<String> theResourceRunnable) {
			Future<String> future = getExecutorServicePool().submit(theResourceRunnable);
			myFutures.add(future);
		}
	}

}
