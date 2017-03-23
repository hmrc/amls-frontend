package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.TimeAtAddress.{SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import scala.collection.JavaConversions._
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class TimeAtAdditionalAddressControllerSpec extends GenericTestHelper with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val timeAtAdditionalAddressController = new TimeAtAdditionalAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)
  val outOfBounds = 99

  "AdditionalAddressController" when {

    val pageTitle = Messages("responsiblepeople.additional_address.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")
    val personName = Some(PersonName("firstname", None, "lastname", None, None))


    "get is called" must {

      "display the persons page when no existing data in save4later" in new Fixture {

        val responsiblePeople = ResponsiblePeople(personName)

        when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))


        val result = timeAtAdditionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
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
      }

      "display the previous home address with UK fields populated" in new Fixture {

        val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

        when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = timeAtAdditionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title must be(pageTitle)
        document.select("input[name=isUK][value=true]").hasAttr("checked") must be(true)
        document.select("input[name=addressLine1]").`val` must be("Line 1")
        document.select("input[name=addressLine2]").`val` must be("Line 2")
        document.select("input[name=addressLine3]").`val` must be("Line 3")
        document.select("input[name=addressLine4]").`val` must be("")
        document.select("input[name=postcode]").`val` must be("AA1 1AA")
      }

      "display the previous home address with non-UK fields populated" in new Fixture {

        val nonUKAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
        val additionalAddress = ResponsiblePersonAddress(nonUKAddress, Some(SixToElevenMonths))
        val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

        when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = timeAtAdditionalAddressController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isUK][value=false]").hasAttr("checked") must be(true)
        document.select("input[name=addressLineNonUK1]").`val` must be("Line 1")
        document.select("input[name=addressLineNonUK2]").`val` must be("Line 2")
        document.select("input[name=addressLineNonUK3]").`val` must be("")
        document.select("input[name=addressLineNonUK4]").`val` must be("")
        document.select("select[name=country] > option[value=ES]").hasAttr("selected") must be(true)
      }
    }

    "post is called" must {
      "respond with SEE_OTHER" when {

        "all the mandatory UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }

        "fail submission on invalid uk address" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line *1",
            "addressLine2" -> "Line &2",
            "postCode" -> "AA1 1AA"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(personName = personName,addressHistory = Some(history))

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

          val document: Document  = Jsoup.parse(contentAsString(result))
          document.title must be(pageTitle)
          val errorCount = 2
          val elementsWithError : Elements = document.getElementsByClass("error-notification")
          elementsWithError.size() must be(errorCount)
          for (ele: Element <- elementsWithError) {
            ele.html() must include(Messages("err.text.validation"))
          }
        }

        "all the mandatory non-UK parameters are supplied" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "Line 1",
            "addressLineNonUK2" -> "Line 2",
            "country" -> "ES"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }

        "fail submission on invalid non uk address" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "Line *1",
            "addressLineNonUK2" -> "Line *2",
            "country" -> "ES"
          )
          val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

          val document: Document  = Jsoup.parse(contentAsString(result))
          val errorCount = 2
          val elementsWithError : Elements = document.getElementsByClass("error-notification")
          elementsWithError.size() must be(errorCount)
          for (ele: Element <- elementsWithError) {
            ele.html() must include(Messages("err.text.validation"))
          }
        }
      }

      "respond with BAD_REQUEST" when {

        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = request.withFormUrlEncodedBody()

          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(line1MissingRequest)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#isUK]").html() must include(Messages("error.required.uk.or.overseas"))
        }

        "the default fields for UK are not supplied" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "",
            "addressLine2" -> "",
            "postCode" -> ""
          )

          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLine2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#postcode]").html() must include(Messages("error.invalid.postcode"))
        }

        "the default fields for overseas are not supplied" in new Fixture {

          val requestWithMissingParams = request.withFormUrlEncodedBody(
            "isUK" -> "false",
            "addressLineNonUK1" -> "",
            "addressLineNonUK2" -> "",
            "country" -> ""
          )

          when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = timeAtAdditionalAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLineNonUK1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLineNonUK2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#country]").html() must include(Messages("error.required.country"))
        }
      }

      "when edit mode is on" when {
        "time at address is less than 1 year" must {
          "redirect to the correct location" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "isUK" -> "true",
              "addressLine1" -> "Line 1",
              "addressLine2" -> "Line 2",
              "postCode" -> "AA1 1AA"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true)(requestWithParams)

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
              "postCode" -> "AA1 1AA"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true)(requestWithParams)

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
              "postCode" -> "AA1 1AA"
            )
            val UKAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
            val additionalAddress = ResponsiblePersonAddress(UKAddress, Some(ZeroToFiveMonths))
            val history = ResponsiblePersonAddressHistory(additionalAddress = Some(additionalAddress))
            val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId, true)(requestWithParams)

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
              "postCode" -> "AA1 1AA"
            )
            val responsiblePeople = ResponsiblePeople()

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

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
              "postCode" -> "AA1 1AA"
            )
            val responsiblePeople = ResponsiblePeople()

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

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
              "postCode" -> "AA1 1AA"
            )
            val responsiblePeople = ResponsiblePeople()

            when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
            when(timeAtAdditionalAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = timeAtAdditionalAddressController.post(RecordId)(requestWithParams)

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
          "postCode" -> "AA1 1AA"
        )

        when(timeAtAdditionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
        when(timeAtAdditionalAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = timeAtAdditionalAddressController.post(outOfBounds, true)(requestWithParams)

        status(result) must be(NOT_FOUND)
      }
    }
  }

}
