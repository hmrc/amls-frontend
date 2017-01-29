package controllers.hvd

import connectors.DataCacheConnector
import models.hvd._
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService: StatusService = mock[StatusService]
    }
  }

  "Get" must {

    val day=15
    val month = 2
    val year = 1956

    val completeModel = Hvd(Some(CashPaymentYes(new LocalDate(year, month, day))),
      Some(Products(Set(Alcohol, Tobacco, Other("test")))),
      Some(ExciseGoods(true)),
      Some(HowWillYouSellGoods(Seq(Wholesale, Retail, Auction))),
      Some(PercentageOfCashPaymentOver15000.Fifth),
      Some(ReceiveCashPayments(Some(PaymentMethods(courier = true, direct = true, other = Some("foo"))))),
        Some(LinkedCashPayments(true))
    )

    "load the summary page when section data is available" in new Fixture {

      val model = Hvd(None)

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))
      when(controller.dataCache.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("summary.checkyouranswers.title"))
    }

    "redirect to the main summary page when section data is unavailable" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCache.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(SEE_OTHER)
    }

    "hide edit link for involved in other, turnover expected from activities and amls turnover expected page" when {
      "application in variation mode" in new Fixture {

        when(controller.dataCache.fetch[Hvd](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(completeModel)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))

        document.getElementsByTag("section").get(0).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(1).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(2).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(3).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(4).getElementsByTag("a").hasClass("change-answer") must be(true)
        document.getElementsByTag("section").get(5).getElementsByTag("a").hasClass("change-answer") must be(false)
        document.getElementsByTag("section").get(6).getElementsByTag("a").hasClass("change-answer") must be(false)
      }
    }

    "show edit link" when {
      "application not in variation mode" in new Fixture {
        when(controller.dataCache.fetch[Hvd](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(completeModel)))

        when(controller.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(NotCompleted))

        val result = controller.get()(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        val elements = document.getElementsByTag("section").iterator
        while(elements.hasNext){
          elements.next().getElementsByTag("a").hasClass("change-answer") must be(true)
        }
      }
    }
  }
}
