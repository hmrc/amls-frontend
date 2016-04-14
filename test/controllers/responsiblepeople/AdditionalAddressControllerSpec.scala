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
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class AdditionalAddressControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

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

  "PreviousHomeAddressController" must {

    "use the correct services" in new Fixture {
      AdditionalAddressController.dataCacheConnector must be(DataCacheConnector)
      AdditionalAddressController.authConnector must be(AMLSAuthConnector)
    }

    "on get() display the persons page when no existing data in keystore" in new Fixture {

      when(additionalAddressController.dataCacheConnector.fetch[ResponsiblePeople](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

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

    "on get() display the previous home address with UK fields populated" in new Fixture {

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

    "on get() display the previous home address with non-UK fields populated" in new Fixture {

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

    "must pass on post with all the mandatory UK parameters supplied" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "isUK" -> "true",
        "addressLine1" -> "Line 1",
        "addressLine2" -> "Line 2",
        "postCode" -> "NE17YH",
        "timeAtAddress" -> "01"
      )

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(additionalAddressController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId)(requestWithParams)
      status(result) must be(SEE_OTHER)
    }

    "must pass on post with all the mandatory non-UK parameters supplied" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "isUK" -> "false",
        "addressLineNonUK1" -> "Line 1",
        "addressLineNonUK2" -> "Line 2",
        "country" -> "ES",
        "timeAtAddress" -> "02"
      )

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(additionalAddressController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId)(requestWithParams)
      status(result) must be(SEE_OTHER)
    }

    "must fail on post if isUK field not supplied" in new Fixture {

      val line1MissingRequest = request.withFormUrlEncodedBody()

      when(additionalAddressController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId)(line1MissingRequest)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#isUK]").html() must include(Messages("error.required.uk.or.overseas"))
    }

    "must fail on post if default fields for UK not supplied" in new Fixture {

      val requestWithMissingParams = request.withFormUrlEncodedBody(
        "isUK" -> "true",
        "addressLine1" -> "",
        "addressLine2" -> "",
        "postCode" -> "",
        "timeAtAddress" -> ""
      )

      when(additionalAddressController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId)(requestWithMissingParams)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#addressLine1]").html() must include(Messages("error.required.address.line1"))
      document.select("a[href=#addressLine2]").html() must include(Messages("error.required.address.line2"))
      document.select("a[href=#postcode]").html() must include(Messages("error.required.postcode"))
      document.select("a[href=#timeAtAddress]").html() must include(Messages("error.required.timeAtAddress"))
    }

    "must fail on post if default fields for overseas not supplied" in new Fixture {

      val requestWithMissingParams = request.withFormUrlEncodedBody(
        "isUK" -> "false",
        "addressLineNonUK1" -> "",
        "addressLineNonUK2" -> "",
        "country" -> "",
        "timeAtAddress" -> ""
      )

      when(additionalAddressController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId)(requestWithMissingParams)
      status(result) must be(BAD_REQUEST)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#addressLineNonUK1]").html() must include(Messages("error.required.address.line1"))
      document.select("a[href=#addressLineNonUK2]").html() must include(Messages("error.required.address.line2"))
      document.select("a[href=#country]").html() must include(Messages("error.required.country"))
      document.select("a[href=#timeAtAddress]").html() must include(Messages("error.required.timeAtAddress"))
    }


    "must go to the correct location when edit mode is on" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "isUK" -> "true",
        "addressLine1" -> "Line 1",
        "addressLine2" -> "Line 2",
        "postCode" -> "NE17YH",
        "timeAtAddress" -> "01"
      )

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(additionalAddressController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId, true)(requestWithParams)
      status(result) must be(SEE_OTHER)
      //TODO: Update this to new location once implementated.
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "must go to the correct location when edit mode is off and time at address is less than 3 years" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "isUK" -> "true",
        "addressLine1" -> "Line 1",
        "addressLine2" -> "Line 2",
        "postCode" -> "NE17YH",
        "timeAtAddress" -> "01"
      )

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(additionalAddressController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId)(requestWithParams)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.AdditionalExtraAddressController.get(RecordId).url))
    }

    "must go to the correct location when edit mode is off and time at address is greater than 3 years" in new Fixture {

      val requestWithParams = request.withFormUrlEncodedBody(
        "isUK" -> "true",
        "addressLine1" -> "Line 1",
        "addressLine2" -> "Line 2",
        "postCode" -> "NE17YH",
        "timeAtAddress" -> "04"
      )

      when(additionalAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(additionalAddressController.dataCacheConnector.save[AddPerson](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = additionalAddressController.post(RecordId)(requestWithParams)
      status(result) must be(SEE_OTHER)
      //TODO: Update this to new location once implementated.
      redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(RecordId).url))
    }

  }

}
