package controllers.tradingpremises

import connectors.DataCacheConnector
import models.TradingPremisesSection
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import models.tradingpremises._
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class MSBServicesControllerSpec extends GenericTestHelper with ScalaFutures with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new MSBServicesController {
      override val dataCacheConnector: DataCacheConnector = self.cache

      override protected def authConnector: AuthConnector = self.authConnector

      override val statusService = mock[StatusService]
    }

    when(controller.statusService.getStatus(any(), any(), any())).thenReturn(Future.successful(SubmissionDecisionRejected))

  }

  "MSBServicesController" must {

    "show an empty form on get with no data in store" in new Fixture {
      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      val result = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = TradingPremises(
        msbServices = Some(
          MsbServices(Set(TransmittingMoney, CurrencyExchange))
        )
      )
      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      val result = controller.get(1)(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[type=checkbox][checked]").size mustBe 2
      document.select("input[value=01]").hasAttr("checked") mustBe true
      document.select("input[value=02]").hasAttr("checked") mustBe true
      document.select(".amls-error-summary").size mustBe 0
    }


    "respond with NOT_FOUND when the index is out of bounds" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(50)(newRequest)
      status(result) must be(NOT_FOUND)
    }

    "respond with NOT_FOUND" when {
      "there is no data at all at that index" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1, false)(request)

        status(result) must be(NOT_FOUND)
      }
    }

    "return a Bad Request with errors on invalid submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "invalid"
      )

      val result = controller.post(1)(newRequest)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe BAD_REQUEST

    }

    "return a redirect to the 'How much Throughput' page on valid submission" in new Fixture {

      val model = TradingPremises(
        msbServices = Some(MsbServices(
          Set(TransmittingMoney)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01"
      )

      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = false)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.PremisesRegisteredController.get(1).url)
    }

    "return a redirect to the 'detailed answers' page when adding 'Transmitting Money' as a service during edit" in new Fixture {

      val currentModel = TradingPremises(
        msbServices = Some(MsbServices(
          Set(ChequeCashingNotScrapMetal)
        ))
      )

      val newModel = currentModel.copy(
        msbServices = Some(MsbServices(
          Set(TransmittingMoney, CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01",
        "msbServices[1]" -> "02",
        "msbServices[2]" -> "03",
        "msbServices[3]" -> "04"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
    }

    "return a redirect to the 'detailed answers' page when adding 'CurrencyExchange' as a service during edit" in new Fixture {

      val currentModel = TradingPremises(
        msbServices = Some(MsbServices(
          Set(ChequeCashingNotScrapMetal)
        ))
      )

      val newModel = currentModel.copy(
        msbServices = Some(MsbServices(
          Set(CurrencyExchange, ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[1]" -> "02",
        "msbServices[2]" -> "03",
        "msbServices[3]" -> "04"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
    }

    "return a redirect to the 'Check Your Answers' page when adding 'Cheque Cashing' as a service during edit" in new Fixture {

      Seq[(MsbService, String)]((ChequeCashingNotScrapMetal, "03"), (ChequeCashingScrapMetal, "04")) foreach {
        case (model, id) =>
          val currentModel = TradingPremises(
            msbServices = Some(MsbServices(
              Set(TransmittingMoney, CurrencyExchange)
            ))
          )

          val newRequest = request.withFormUrlEncodedBody(
            "msbServices[1]" -> "01",
            "msbServices[2]" -> "02",
            "msbServices[3]" -> id
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
            (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises(msbServices = None)))))

          when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

          val result = controller.post(1, edit = true)(newRequest)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
      }
    }

    "set the hasChanged flag to true" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[1]" -> "01",
        "msbServices[2]" -> "02",
        "msbServices[3]" -> "03"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1)(newRequest)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.PremisesRegisteredController.get(1).url))

      verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
        any(),
        meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
          hasChanged = true,
          msbServices = Some(MsbServices(Set(TransmittingMoney, CurrencyExchange, ChequeCashingNotScrapMetal)))
        ))))(any(), any(), any())
    }

    "redirect to the dateOfChange page when the services have changed for a variation" in new Fixture {

      val model = TradingPremises(
        lineId = Some(1),
        msbServices = Some(MsbServices(
          Set(TransmittingMoney)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01",
        "msbServices[1]" -> "02"
      )

      when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
    }


    "redirect to the dateOfChange page when the services have changed for a ready for renewal status" in new Fixture {

      val model = TradingPremises(
        lineId = Some(1),
        msbServices = Some(MsbServices(
          Set(TransmittingMoney)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01",
        "msbServices[1]" -> "02"
      )

      when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(ReadyForRenewal(None))

      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
    }


    "redirect to the dateOfChange page when the MsbServices haven't changed, but a change from previous services page has been flagged" in new Fixture {

      val model = TradingPremises(
        lineId = Some(1),
        msbServices = Some(MsbServices(
          Set(TransmittingMoney)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01"
      )

      when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = true, changed = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.WhatDoesYourBusinessDoController.dateOfChange(1).url)
    }


    "redirect to the summary controller when editing, and the services have changed for a record that hasn't been submitted yet" in new Fixture {

      val model = TradingPremises(
        lineId = None,                    // record hasn't been submitted
        msbServices = Some(MsbServices(
          Set(TransmittingMoney)
        ))
      )

      val newRequest = request.withFormUrlEncodedBody(
        "msbServices[0]" -> "01",
        "msbServices[1]" -> "02"
      )

      when(controller.statusService.getStatus(any(), any(), any())) thenReturn Future.successful(SubmissionDecisionApproved)

      when(cache.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))

      when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

      val result = controller.post(1, edit = true)(newRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
    }

  }
}
