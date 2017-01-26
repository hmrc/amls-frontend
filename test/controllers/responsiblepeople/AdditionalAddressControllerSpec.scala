package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.TimeAtAddress.{ZeroToFiveMonths, SixToElevenMonths}
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class AdditionalAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self =>

    val additionalAddressController = new AdditionalAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "AdditionalAddressController" when {

    "get is called" must {

      "display the persons page when no existing data in keystore" in new Fixture {

        val responsiblePeople = ResponsiblePeople()

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))


        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(true)
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(false)
        document.select("input[name=addressLine1]").`val` must be("")
        document.select("input[name=addressLine2]").`val` must be("")
        document.select("input[name=addressLine3]").`val` must be("")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=addressLineNonUK1]").`val` must be("")
        document.select("input[name=addressLineNonUK2]").`val` must be("")
        document.select("input[name=addressLineNonUK3]").`val` must be("")
        document.select("input[name=addressLineNonUK4]").`val` must be("")
        document.select("input[name=postcode]").`val` must be("")
        document.select("input[name=country]").`val` must be("")
        document.select("input[name=timeAtAddress][value=01]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=04]").hasAttr("checked") must be(false)
      }

      "display the previous home address with UK fields populated" in new Fixture {

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
        val additionalAddress = ResponsiblePersonAddress(UKAddress, ZeroToFiveMonths)
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(true)
        document.select("input[name=addressLine1]").`val` must be("Line 1")
        document.select("input[name=addressLine2]").`val` must be("Line 2")
        document.select("input[name=addressLine3]").`val` must be("Line 3")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=postcode]").`val` must be("NE17YH")
        document.select("input[name=timeAtAddress][value=01]").hasAttr("checked") must be(true)
      }

      "display the previous home address with non-UK fields populated" in new Fixture {

        val nonUKAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
        val additionalAddress = ResponsiblePersonAddress(nonUKAddress, SixToElevenMonths)
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = additionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
        document.select("input[name=addressLineNonUK1]").`val` must be("Line 1")
        document.select("input[name=addressLineNonUK2]").`val` must be("Line 2")
        document.select("input[name=addressLineNonUK3]").`val` must be("")
        document.select("input[name=addressLineNonUK4]").`val` must be("")
        document.select("select[name=country] > option[value=ES]").hasAttr("selected") must be(true)
        document.select("input[name=timeAtAddress][value=02]").hasAttr("checked") must be(true)
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {

        "all the mandatory UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "NE17YH",
            "timeAtAddress" -> "04"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, ZeroToFiveMonths)
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }

        "all the mandatory non-UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "Line 1",
            "addressLineNonUK2" -> "Line 2",
            "country" -> "ES",
            "timeAtAddress" -> "02"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, ZeroToFiveMonths)
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }
      }

      "respond with BAD_REQUEST" when {

        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = request.withFormUrlEncodedBody()

          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#isUK]").html() must include(Messages("error.required.uk.or.overseas"))
        }

        "the default fields for UK are not supplied" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "",
            "addressLine2" -> "",
            "postCode" -> "",
            "timeAtAddress" -> ""
          )

          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLine2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#postcode]").html() must include(Messages("error.required.postcode"))
          document.select("a[href=#timeAtAddress]").html() must include(Messages("error.required.timeAtAddress"))
        }

        "the default fields for overseas are not supplied" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "",
            "addressLineNonUK2" -> "",
            "country" -> "",
            "timeAtAddress" -> ""
          )

          when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = additionalAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLineNonUK1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLineNonUK2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#country]").html() must include(Messages("error.required.country"))
          document.select("a[href=#timeAtAddress]").html() must include(Messages("error.required.timeAtAddress"))
        }
      }

      "when edit mode is on" when {
        "time at address is less than 1 year" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode" -> "NE17YH",
              "timeAtAddress" -> "01"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, ZeroToFiveMonths)
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = additionalAddressController.post(RecordId, true)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AdditionalExtraAddressController.get(RecordId, true).url))
          }
        }

        "time at address is OneToThreeYears" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode" -> "NE17YH",
              "timeAtAddress" -> "03"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, ZeroToFiveMonths)
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = additionalAddressController.post(RecordId, true)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          }
        }
        "time at address is ThreeYearsPlus" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode" -> "NE17YH",
              "timeAtAddress" -> "04"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "NE17YH")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, ZeroToFiveMonths)
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = additionalAddressController.post(RecordId, true)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId).url))
          }
        }
      }

      "when edit mode is off" when {
        "time at address is less than 1 year" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode" -> "NE17YH",
              "timeAtAddress" -> "01"
            )
            val responsiblePeople = ResponsiblePeople()

            when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = additionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.AdditionalExtraAddressController.get(RecordId).url))
          }
        }

        "time at address is OneToThreeYears" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode" -> "NE17YH",
              "timeAtAddress" -> "03"
            )
            val responsiblePeople = ResponsiblePeople()

            when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = additionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
          }
        }

        "time at address is ThreeYearsPlus" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode" -> "NE17YH",
              "timeAtAddress" -> "04"
            )
            val responsiblePeople = ResponsiblePeople()

            when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(additionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = additionalAddressController.post(RecordId)(requestWithParams)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
          }
        }
      }
    }


  }

  it must {
    "use the correct services" in new Fixture {
      AdditionalAddressController.dataCacheConnector must be(DataCacheConnector)
      AdditionalAddressController.authConnector must be(AMLSAuthConnector)
    }
  }


  it must {
    "respond with NOT_FOUND" when {
      "given an index out of bounds in edit mode" in new Fixture {

        val requestWithParams = request.withFormUrlEncodedBody(
          "isUK" -> "true",
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "postCode" -> "NE17YH",
          "timeAtAddress" -> "04"
        )

        when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
        when(additionalAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = additionalAddressController.post(50, true)(requestWithParams)

        status(result) must be(NOT_FOUND)
      }
    }
  }

}
