package controllers.responsiblepeople

import java.util.UUID

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.responsiblepeople._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class PersonNameControllerSpec extends GenericTestHelper with MockitoSugar {

  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]
  val RecordId = 1

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val personNameController = new PersonNameController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "PersonNameController" when {

    "get is called" must {

      "display the persons page with blank fields" in new Fixture {

        when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

        val result = personNameController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=firstName]").`val` must be("")
        document.select("input[name=middleName]").`val` must be("")
        document.select("input[name=lastName]").`val` must be("")
        document.getElementById("hasPreviousName-true").hasAttr("checked") must be(false)
        document.getElementById("hasPreviousName-false").hasAttr("checked") must be(false)
        document.select("input[name=previous.firstName]").`val` must be("")
        document.select("input[name=previous.middleName]").`val` must be("")
        document.select("input[name=previous.lastName]").`val` must be("")
        document.select("input[name=previous.date.day]").`val` must be("")
        document.select("input[name=previous.date.month]").`val` must be("")
        document.select("input[name=previous.date.year]").`val` must be("")
        document.getElementById("hasOtherNames-true").hasAttr("checked") must be(false)
        document.getElementById("hasOtherNames-false").hasAttr("checked") must be(false)
        document.select("input[name=otherNames]").`val` must be("")
      }

      "display the persons page with fields populated" in new Fixture {

        val addPerson = PersonName(
          firstName = "first",
          middleName = Some("middle"),
          lastName = "last",
          previousName = Some(
            PreviousName(
              firstName = Some("oldFirst"),
              middleName = Some("oldMiddle"),
              lastName = Some("oldLast"),
              // scalastyle:off magic.number
              date = new LocalDate(1990, 2, 24)
            )
          ),
          otherNames = Some("Doc")
        )

        val responsiblePeople = ResponsiblePeople(Some(addPerson))

        when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(responsiblePeople))))

        val result = personNameController.get(RecordId)(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))

        document.select("input[name=firstName]").`val` must be("first")
        document.select("input[name=middleName]").`val` must be("middle")
        document.select("input[name=lastName]").`val` must be("last")
        document.getElementById("hasPreviousName-true").hasAttr("checked") must be(true)
        document.getElementById("hasPreviousName-false").hasAttr("checked") must be(false)
        document.select("input[name=previous.firstName]").`val` must be("oldFirst")
        document.select("input[name=previous.middleName]").`val` must be("oldMiddle")
        document.select("input[name=previous.lastName]").`val` must be("oldLast")
        document.select("input[name=previous.date.day]").`val` must be("24")
        document.select("input[name=previous.date.month]").`val` must be("2")
        document.select("input[name=previous.date.year]").`val` must be("1990")
        document.getElementById("hasOtherNames-true").hasAttr("checked") must be(true)
        document.getElementById("hasOtherNames-false").hasAttr("checked") must be(false)
        document.select("input[name=otherNames]").`val` must be("Doc")
      }

      "display Not Found" when {
        "ResponsiblePeople model cannot be found" in new Fixture {
          when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = personNameController.get(RecordId)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }

    "post is called" when {

      "form is valid" must {
        "go to PersonResidentTypeController" when {
          "edit is false" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "firstName" -> "first",
              "middleName" -> "middle",
              "lastName" -> "last",
              "hasPreviousName" -> "true",
              "previous.firstName" -> "oldFirst",
              "previous.middleName" -> "oldMiddle",
              "previous.lastName" -> "oldLast",
              "previous.date.year" -> "1990",
              "previous.date.month" -> "02",
              "previous.date.day" -> "24",
              "hasOtherNames" -> "true",
              "otherNames" -> "Doc"
            )

            when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

            when(personNameController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = personNameController.post(RecordId)(requestWithParams)
            status(result) must be(SEE_OTHER)
          }
        }
        "go to DetailedAnswersController" when {
          "edit is true" in new Fixture {

            val requestWithParams = request.withFormUrlEncodedBody(
              "firstName" -> "first",
              "middleName" -> "middle",
              "lastName" -> "last",
              "hasPreviousName" -> "true",
              "previous.firstName" -> "oldFirst",
              "previous.middleName" -> "oldMiddle",
              "previous.lastName" -> "oldLast",
              "previous.date.year" -> "1990",
              "previous.date.month" -> "02",
              "previous.date.day" -> "24",
              "hasOtherNames" -> "true",
              "otherNames" -> "Doc"
            )

            when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

            when(personNameController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(emptyCache))

            val result = personNameController.post(RecordId, true)(requestWithParams)
            status(result) must be(SEE_OTHER)
          }
        }
      }

      "form is invalid" must {
        "return BAD_REQUEST" in new Fixture {

          val firstNameMissingInRequest = request.withFormUrlEncodedBody(
            "lastName" -> "Doe",
            "isKnownByOtherNames" -> "false"
          )

          when(personNameController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = personNameController.post(RecordId)(firstNameMissingInRequest)
          status(result) must be(BAD_REQUEST)

        }

      }

      "model cannot be found with given index" must {
        "return NOT_FOUND" in new Fixture {

          val requestWithParams = request.withFormUrlEncodedBody(
            "firstName" -> "John",
            "lastName" -> "Doe",
            "hasPreviousName" -> "false",
            "hasOtherNames" -> "false"
          )

          when(personNameController.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          when(personNameController.dataCacheConnector.save[PersonName](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = personNameController.post(2)(requestWithParams)
          status(result) must be(NOT_FOUND)
        }
      }

    }

  }


  it must {
    "use the correct services" in new Fixture {
      PersonNameController.dataCacheConnector must be(DataCacheConnector)
      PersonNameController.authConnector must be(AMLSAuthConnector)
    }
  }

}
