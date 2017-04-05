package services

import connectors.DataCacheConnector
import models.Country
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import models.renewal._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RenewalServiceSpec extends GenericTestHelper with MockitoSugar {

  implicit val hc = HeaderCarrier()

  trait Fixture extends AuthorisedFixture {

    val dataCache = mock[DataCacheConnector]
    implicit val authContext = mock[AuthContext]

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[DataCacheConnector].to(dataCache))
      .build()

    val service = injector.instanceOf[RenewalService]

    val completeModel = Renewal(
      Some(InvolvedInOtherYes("test")),
      Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),
      Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(MsbThroughput("01")),
      Some(SendTheLargestAmountsOfMoney(Country("us", "US"))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(CETransactions("123")),
      // Add other models here
      true)

  }

  "The renewal service" must {

    "return the correct section" when {

      "the renewal hasn't been started" in new Fixture {

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(None)

        val section = await(service.getSection)

        section mustBe Section("renewal", NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())

      }

      "the renewal is complete and has been started" in new Fixture {
        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(completeModel))

        val section = await(service.getSection)

        section mustBe Section("renewal", Completed, hasChanged = true, controllers.renewal.routes.SummaryController.get())

      }

      "the renewal model is not complete" in new Fixture {

        val renewal = mock[Renewal]
        when(renewal.hasChanged) thenReturn true
        when(renewal.isComplete) thenReturn false

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(renewal))

        val section = await(service.getSection)

        section mustBe Section("renewal", Started, hasChanged = true, controllers.renewal.routes.WhatYouNeedController.get())

      }

      "the renewal model is not complete and not started" in new Fixture {
        val renewal = Renewal(None)

        when {
          dataCache.fetch[Renewal](eqTo(Renewal.key))(any(), any(), any())
        } thenReturn Future.successful(Some(renewal))

        val section = await(service.getSection)

        section mustBe Section("renewal", NotStarted, hasChanged = false, controllers.renewal.routes.WhatYouNeedController.get())
      }

    }

  }

}
