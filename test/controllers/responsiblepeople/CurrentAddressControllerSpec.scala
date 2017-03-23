package controllers.responsiblepeople

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.Country
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import models.responsiblepeople._
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeApplication
import scala.collection.JavaConversions._
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class CurrentAddressControllerSpec extends GenericTestHelper with MockitoSugar {


  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val currentAddressController = new CurrentAddressController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]

    }
  }
  override lazy val app = FakeApplication(additionalConfiguration = Map("Test.microservice.services.feature-toggle.release7" -> true))

  val emptyCache = CacheMap("", Map.empty)

  "CurrentAddressController" when {

    val pageTitle = Messages("responsiblepeople.wherepersonlives.title", "firstname lastname") + " - " +
      Messages("summary.responsiblepeople") + " - " +
      Messages("title.amls") + " - " + Messages("title.gov")

    val personName = Some(PersonName("firstname", None, "lastname", None, None))

    "get is called" must {

      "respond with NOT_FOUND when called with an index that is out of bounds" in new Fixture {
        val responsiblePeople = ResponsiblePeople()

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(40)(request)
        status(result) must be(NOT_FOUND)
      }

      "display the persons page when no existing data in save4later" in new Fixture {

        val responsiblePeople = ResponsiblePeople(personName)

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
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
        document.select("input[name=timeAtAddress][value=01]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=02]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=03]").hasAttr("checked") must be(false)
        document.select("input[name=timeAtAddress][value=04]").hasAttr("checked") must be(false)
      }

      "display the previous home address with UK fields populated" in new Fixture {

        val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
        val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
        val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(personName = personName,addressHistory = Some(history))

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
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

        val nonukAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
        val additionalAddress = ResponsiblePersonCurrentAddress(nonukAddress, Some(SixToElevenMonths))
        val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
        val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = currentAddressController.get(RecordId)(request)
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
            "postCode" -> "AA1 1AA",
            "timeAtAddress" -> "04"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }

        "fail submission on invalid address" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line &1",
            "addressLine2" -> "Line *2",
            "postCode" -> "AA1 1AA",
            "timeAtAddress" -> "04"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(personName = personName, addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)
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
            "country" -> "ES",
            "timeAtAddress" -> "02"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithParams)

          status(result) must be(SEE_OTHER)
        }
      }

      "respond with BAD_REQUEST" when {

        "isUK field is not supplied" in new Fixture {

          val line1MissingRequest = request.withFormUrlEncodedBody()

          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(line1MissingRequest)
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

          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLine1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLine2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#postcode]").html() must include(Messages("error.invalid.postcode"))
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

          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(RecordId)(requestWithMissingParams)
          status(result) must be(BAD_REQUEST)

          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("a[href=#addressLineNonUK1]").html() must include(Messages("error.required.address.line1"))
          document.select("a[href=#addressLineNonUK2]").html() must include(Messages("error.required.address.line2"))
          document.select("a[href=#country]").html() must include(Messages("error.required.country"))
          document.select("a[href=#timeAtAddress]").html() must include(Messages("error.required.timeAtAddress"))
        }
      }

      "respond with NOT_FOUND" when {
        "given an out of bounds index" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "isUK" -> "true",
            "addressLine1" -> "Line 1",
            "addressLine2" -> "Line 2",
            "postCode" -> "AA1 1AA",
            "timeAtAddress" -> "01"
          )
          val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
          val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
          val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
          val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

          when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
          when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(currentAddressController.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          val result = currentAddressController.post(40, true)(requestWithParams)

          status(result) must be(NOT_FOUND)
        }
      }

      "when the service status is not yet submitted" when {

        "edit mode is on" when {
          "time at address is less than 1 year" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "isUK" -> "true",
                "addressLine1" -> "Line 1",
                "addressLine2" -> "Line 2",
                "postCode" -> "AA1 1AA",
                "timeAtAddress" -> "01"
              )
              val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
              val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
              val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
              val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

              when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(currentAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = currentAddressController.post(RecordId, true)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(RecordId, true).url))
            }
          }

          "time at address is OneToThreeYears" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "isUK" -> "true",
                "addressLine1" -> "Line 1",
                "addressLine2" -> "Line 2",
                "postCode" -> "AA1 1AA",
                "timeAtAddress" -> "03"
              )
              val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
              val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
              val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
              val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

              when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(currentAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = currentAddressController.post(RecordId, true)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true).url))
            }
          }
          "time at address is ThreeYearsPlus" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "isUK" -> "true",
                "addressLine1" -> "Line 1",
                "addressLine2" -> "Line 2",
                "postCode" -> "AA1 1AA",
                "timeAtAddress" -> "04"
              )
              val ukAddress = PersonAddressUK("Line 1", "Line 2", Some("Line 3"), None, "AA1 1AA")
              val additionalAddress = ResponsiblePersonCurrentAddress(ukAddress, Some(ZeroToFiveMonths))
              val history = ResponsiblePersonAddressHistory(currentAddress = Some(additionalAddress))
              val responsiblePeople = ResponsiblePeople(addressHistory = Some(history))

              when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))

              when(currentAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = currentAddressController.post(RecordId, true)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(RecordId, true).url))
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
                "postCode" -> "AA1 1AA",
                "timeAtAddress" -> "01"
              )
              val responsiblePeople = ResponsiblePeople()

              when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(currentAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = currentAddressController.post(RecordId)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(RecordId).url))
            }
          }

          "time at address is OneToThreeYears" must {
            "redirect to the correct location" in new Fixture {

              val requestWithParams = request.withFormUrlEncodedBody(
                "isUK" -> "true",
                "addressLine1" -> "Line 1",
                "addressLine2" -> "Line 2",
                "postCode" -> "AA1 1AA",
                "timeAtAddress" -> "03"
              )
              val responsiblePeople = ResponsiblePeople()

              when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(currentAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = currentAddressController.post(RecordId)(requestWithParams)

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
                "postCode" -> "AA1 1AA",
                "timeAtAddress" -> "04"
              )
              val responsiblePeople = ResponsiblePeople()

              when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(responsiblePeople))))
              when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(currentAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionReadyForReview))

              val result = currentAddressController.post(RecordId)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(RecordId).url))
            }
          }
        }
      }

      "when the service status is approved" when {
        "the responsible person has previously been submitted and therefore has a lineID" when {
          "editing an existing address and the address has been changed" must {
            "redirect to the date of change controller" in new Fixture {
              val requestWithParams = request.withFormUrlEncodedBody(
                "isUK" -> "true",
                "addressLine1" -> "newline1",
                "addressLine2" -> "newline2",
                "postCode" -> "AB1 2CD",
                "timeAtAddress" -> "04"
              )

              val originalResponsiblePeople = ResponsiblePeople(
                addressHistory = Some(ResponsiblePersonAddressHistory(
                  currentAddress = Some(ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(OneToThreeYears), None)
                  )
                )),
                lineId = Some(1)
              )

              when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
              when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                .thenReturn(Future.successful(emptyCache))
              when(currentAddressController.statusService.getStatus(any(), any(), any()))
                .thenReturn(Future.successful(SubmissionDecisionApproved))

              val result = currentAddressController.post(RecordId, true)(requestWithParams)

              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.CurrentAddressDateOfChangeController.get(1, true).url))

            }
          }
          "editing an existing address and the address has not changed" when {
            "time at address is less than 1 year" must {
              "redirect to the additional address controller" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "isUK" -> "true",
                  "addressLine1" -> "line1",
                  "addressLine2" -> "line2",
                  "postCode" -> "AB1 2CD",
                  "timeAtAddress" -> "01"
                )

                val originalResponsiblePeople = ResponsiblePeople(
                  addressHistory = Some(ResponsiblePersonAddressHistory(
                    currentAddress = Some(
                      ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(ZeroToFiveMonths), None)
                    )
                  )),
                  lineId = Some(1)
                )

                when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(currentAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = currentAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(1, true).url))
              }
            }
            "time at address is more than 1 year" must {
              "redirect to the detailed answers controller" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "isUK" -> "true",
                  "addressLine1" -> "line1",
                  "addressLine2" -> "line2",
                  "postCode" -> "AB1 2CD",
                  "timeAtAddress" -> "03"
                )

                val originalResponsiblePeople = ResponsiblePeople(
                  addressHistory = Some(ResponsiblePersonAddressHistory(
                    currentAddress = Some(ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(OneToThreeYears), None)
                    )
                  )),
                  lineId = Some(1)
                )

                when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(currentAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = currentAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1, true).url))
              }
            }
          }
        }
        "the responsible person has not previously been submitted and therefore has not got a lineID" when {

          "adding a new responsible person (therefore edit is false)" when {
            "time at address is less than 1 year" must {
              "redirect to the additional address controller" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "isUK" -> "true",
                  "addressLine1" -> "line1",
                  "addressLine2" -> "line2",
                  "postCode" -> "AB1 2CD",
                  "timeAtAddress" -> "01"
                )

                val originalResponsiblePeople = ResponsiblePeople()

                when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(currentAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = currentAddressController.post(RecordId, false)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(1, false).url))
              }
            }
            "time at address is more than 1 year" must {
              "redirect to the PositionWithinBusinessController" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "isUK" -> "true",
                  "addressLine1" -> "line1",
                  "addressLine2" -> "line2",
                  "postCode" -> "AB1 2CD",
                  "timeAtAddress" -> "03"
                )

                val originalResponsiblePeople = ResponsiblePeople()

                when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(currentAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = currentAddressController.post(RecordId, false)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.PositionWithinBusinessController.get(1, false).url))
              }
            }
          }
          "editing a previously added responsible person (therefore edit is true)" when {
            "time at address is less than 1 year" must {
              "redirect to the additional address controller" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "isUK" -> "true",
                  "addressLine1" -> "line1new",
                  "addressLine2" -> "line2new",
                  "postCode" -> "AB1 2CD",
                  "timeAtAddress" -> "01"
                )

                val originalResponsiblePeople = ResponsiblePeople(
                  addressHistory = Some(ResponsiblePersonAddressHistory(
                    currentAddress = Some(ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(ZeroToFiveMonths), None)
                    )
                  )),
                  lineId = None
                )

                when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(currentAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = currentAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.AdditionalAddressController.get(1, true).url))
              }
            }
            "time at address is more than 1 year" must {
              "redirect to the DetailedAnswersController" in new Fixture {
                val requestWithParams = request.withFormUrlEncodedBody(
                  "isUK" -> "true",
                  "addressLine1" -> "line1new",
                  "addressLine2" -> "line2new",
                  "postCode" -> "AB1 2CD",
                  "timeAtAddress" -> "03"
                )

                val originalResponsiblePeople = ResponsiblePeople(
                  addressHistory = Some(ResponsiblePersonAddressHistory(
                    currentAddress = Some(ResponsiblePersonCurrentAddress(PersonAddressUK("line1", "line2", None, None, "AB1 2CD"), Some(OneToThreeYears), None)
                    )
                  )),
                  lineId = None
                )

                when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
                  .thenReturn(Future.successful(Some(Seq(originalResponsiblePeople))))
                when(currentAddressController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
                  .thenReturn(Future.successful(emptyCache))
                when(currentAddressController.statusService.getStatus(any(), any(), any()))
                  .thenReturn(Future.successful(SubmissionDecisionApproved))

                val result = currentAddressController.post(RecordId, true)(requestWithParams)

                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(routes.DetailedAnswersController.get(1, true).url))
              }
            }
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
          "postCode" -> "AA1 1AA",
          "timeAtAddress" -> "04"
        )

        when(currentAddressController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))
        when(currentAddressController.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = currentAddressController.post(50, true)(requestWithParams)

        status(result) must be(NOT_FOUND)
      }
    }
  }

}


