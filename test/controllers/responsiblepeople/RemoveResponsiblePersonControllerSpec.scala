package controllers.responsiblepeople

import connectors.DataCacheConnector
import forms.EmptyForm
import models.Country
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import models.responsiblepeople._
import models.status._
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerSuite
import services.{AuthEnrolmentsService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper, StatusConstants}
import play.api.test.Helpers._
import org.mockito.Matchers.{eq => meq, _}
import play.api.i18n.Messages

import scala.concurrent.Future

class RemoveResponsiblePersonControllerSpec extends GenericTestHelper
  with MustMatchers with MockitoSugar with ScalaFutures with PropertyChecks {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new RemoveResponsiblePersonController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val statusService: StatusService =  mock[StatusService]
      override val authConnector = self.authConnector
      override val authEnrolmentsService: AuthEnrolmentsService = mock[AuthEnrolmentsService]
    }
  }

  "RemoveResponsiblePersonController" when {
    "get is called" when {
      "the submission status is NotCompleted" must {
        "respond with OK when the index is valid" in new Fixture {

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(Some(PersonName("firstName", None, "lastName", None, None)))))))

          val result = controller.get(1, false)(request)

          status(result) must be(OK)

        }
        "respond with NOT_FOUND when the index is out of bounds" in new Fixture {

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controller.get(100, false)(request)

          status(result) must be(NOT_FOUND)

        }
      }
      "the submission status is SubmissionDecisionApproved" must {
        "respond with OK when the index is valid" in new Fixture {

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople(Some(PersonName("firstName", None, "lastName", None, None)), lineId = Some(4444))))))

          val result = controller.get(1, false)(request)

          status(result) must be(OK)

        }
        "respond with NOT_FOUND when the index is out of bounds" in new Fixture {

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))
          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(ResponsiblePeople()))))

          val result = controller.get(100, false)(request)

          status(result) must be(NOT_FOUND)

        }
        "respond with OK without showing endDate form when RP does not have lineId" in new Fixture{

          val rp = ResponsiblePeople(
            Some(PersonName("firstName", None, "lastName", None, None))
          )

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(rp))))

          val result = controller.get(1, false)(request)

          status(result) must be(OK)

          contentAsString(result) must not include Messages("responsiblepeople.remove.responsible.person.enddate.lbl")
        }
      }
      "the submission status is SubmissionReadyForReview" must {
        "respond with OK without showing endDate form when RP does not have lineId" in new Fixture{

          val rp = ResponsiblePeople(
            Some(PersonName("firstName", None, "lastName", None, None))
          )

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(rp))))

          val result = controller.get(1, false)(request)

          status(result) must be(OK)

          contentAsString(result) must not include Messages("responsiblepeople.remove.responsible.person.enddate.lbl")

        }
        "respond with OK without showing endDate form when RP does have lineId" in new Fixture{

          val rp = ResponsiblePeople(
            personName = Some(PersonName("firstName", None, "lastName", None, None)),
            lineId = Some(4444)
          )

          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(rp))))

          val result = controller.get(1, false)(request)

          status(result) must be(OK)

          contentAsString(result) must not include Messages("responsiblepeople.remove.responsible.person.enddate.lbl")

        }
      }
    }

    "remove is called" must {
      "respond with SEE_OTHER" when {
        "removing a responsible person from an application with status NotCompleted" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(NotCompleted))

          val result = controller.remove(1, false, "John Envy Doe")(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CheckYourAnswersController.get().url))

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePeople]](any(), meq(Seq(
            CompleteResponsiblePeople2,
            CompleteResponsiblePeople3
          )))(any(), any(), any())
        }

        "removing a responsible person from an application with status SubmissionReady" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.remove(1, false, "John Envy Doe")(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CheckYourAnswersController.get().url))

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePeople]](any(), meq(Seq(
            CompleteResponsiblePeople2,
            CompleteResponsiblePeople3
          )))(any(), any(), any())
        }

        "removing a responsible person from an application with status SubmissionReady and redirect to your answers page" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReady))

          val result = controller.remove(1, true, "John Envy Doe")(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.YourAnswersController.get().url))

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePeople]](any(), meq(Seq(
            CompleteResponsiblePeople2,
            CompleteResponsiblePeople3
          )))(any(), any(), any())
        }

        "removing a responsible person from an application with status SubmissionReadyForReview" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionReadyForReview))


          val result = controller.remove(1, false, "John Envy Doe")(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CheckYourAnswersController.get().url))

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePeople]](any(), meq(Seq(
            CompleteResponsiblePeople1.copy(status = Some(StatusConstants.Deleted), hasChanged = true),
            CompleteResponsiblePeople2,
            CompleteResponsiblePeople3
          )))(any(), any(), any())
        }

        "removing a responsible person from an application with status SubmissionDecisionApproved" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)
          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "1",
            "endDate.month" -> "1",
            "endDate.year" -> "2006"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))


          val result = controller.remove(1, complete = false, "John Envy Doe")(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.CheckYourAnswersController.get().url))

          verify(controller.dataCacheConnector).save[Seq[ResponsiblePeople]](any(), meq(Seq(
            CompleteResponsiblePeople1.copy(status = Some(StatusConstants.Deleted), hasChanged = true,
              endDate = Some(ResponsiblePersonEndDate(new LocalDate(2006, 1, 1)))),
            CompleteResponsiblePeople2,
            CompleteResponsiblePeople3
          )))(any(), any(), any())
        }

        "removing a responsible person from an application with no date" in new Fixture {
          val emptyCache = CacheMap("", Map.empty)

          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "",
            "endDate.month" -> "",
            "endDate.year" -> ""
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(CompleteResponsiblePeople1.copy(lineId = None)))))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true, "person Name")(newRequest)
          status(result) must be(SEE_OTHER)

        }

      }

      "respond with BAD_REQUEST" when {
        "removing a responsible person from an application with no date" in new Fixture {
          val emptyCache = CacheMap("", Map.empty)

          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "",
            "endDate.month" -> "",
            "endDate.year" -> ""
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true, "person Name")(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.expected.jodadate.format"))

        }

        "removing a responsible person from an application given a year which is too short" in new Fixture {
          val emptyCache = CacheMap("", Map.empty)

          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "24",
            "endDate.month" -> "2",
            "endDate.year" -> "16"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true, "person Name")(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.expected.jodadate.format"))

        }

        "removing a responsible person from an application given a year which is too long" in new Fixture {
          val emptyCache = CacheMap("", Map.empty)

          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "24",
            "endDate.month" -> "2",
            "endDate.year" -> "10166"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true, "person Name")(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.expected.jodadate.format"))

        }

        "removing a trading premises from an application with future date" in new Fixture {
          val emptyCache = CacheMap("", Map.empty)

          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "15",
            "endDate.month" -> "1",
            "endDate.year" -> "2020"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(ResponsiblePeopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true, "person Name")(newRequest)
          status(result) must be(BAD_REQUEST)
          contentAsString(result) must include(Messages("error.future.date"))

        }

        "removing a responsible person from an application with end date before position start date" in new Fixture {

          val emptyCache = CacheMap("", Map.empty)

          val position = Positions(Set(InternalAccountant), Some(new LocalDate(1999, 5, 1)))
          val peopleList = Seq(CompleteResponsiblePeople1.copy(positions = Some(position)))

          val newRequest = request.withFormUrlEncodedBody(
            "endDate.day" -> "15",
            "endDate.month" -> "1",
            "endDate.year" -> "1998"
          )

          when(controller.dataCacheConnector.fetch[Seq[ResponsiblePeople]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(peopleList)))
          when(controller.dataCacheConnector.save[Seq[ResponsiblePeople]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))
          when(controller.statusService.getStatus(any(), any(), any()))
            .thenReturn(Future.successful(SubmissionDecisionApproved))

          val result = controller.remove(1, true, "person Name")(newRequest)
          status(result) must be(BAD_REQUEST)
          //contentAsString(result) must include(Messages("error.expected.future.date.after.start"))

        }
      }

    }
  }

  private val residence = UKResidence("AA3464646")
  private val residenceCountry = Country("United Kingdom", "GB")
  private val residenceNationality = Country("United Kingdom", "GB")
  private val currentPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "NE981ZZ")
  private val currentAddress = ResponsiblePersonCurrentAddress(currentPersonAddress, Some(ZeroToFiveMonths))
  private val additionalPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "NE15GH")
  private val additionalAddress = ResponsiblePersonAddress(additionalPersonAddress, Some(ZeroToFiveMonths))
  //scalastyle:off magic.number
  val previousName = PreviousName(Some("Matt"), Some("Mc"), Some("Fly"), new LocalDate(1990, 2, 24))
  val personName = PersonName("John", Some("Envy"), "Doe", Some(previousName), Some("name"))
  val personResidenceType = PersonResidenceType(residence, residenceCountry, Some(residenceNationality))
  val saRegistered = SaRegisteredYes("0123456789")
  val contactDetails = ContactDetails("07702743555", "test@test.com")
  val addressHistory = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
  val vatRegistered = VATRegisteredNo
  val training = TrainingYes("test")
  val experienceTraining = ExperienceTrainingYes("Some training")

  //scalastyle:off magic.number
  val positions = Positions(Set(BeneficialOwner, InternalAccountant),Some(new LocalDate(2005, 3, 15)))

  val CompleteResponsiblePeople1 = ResponsiblePeople(
    Some(personName),
    Some(personResidenceType),
    Some(contactDetails),
    Some(addressHistory),
    Some(positions),
    Some(saRegistered),
    Some(vatRegistered),
    Some(experienceTraining),
    Some(training),
    Some(true),
    false,
    Some(1),
    Some("test")
  )
  val CompleteResponsiblePeople2 = ResponsiblePeople(
    Some(personName),
    Some(personResidenceType),
    Some(contactDetails),
    Some(addressHistory),
    Some(positions),
    Some(saRegistered),
    Some(vatRegistered),
    Some(experienceTraining),
    Some(training),
    Some(true),
    false,
    Some(1),
    Some("test")
  )
  val CompleteResponsiblePeople3 = ResponsiblePeople(
    Some(personName),
    Some(personResidenceType),
    Some(contactDetails),
    Some(addressHistory),
    Some(positions),
    Some(saRegistered),
    Some(vatRegistered),
    Some(experienceTraining),
    Some(training),
    Some(true),
    false,
    Some(1),
    Some("test")
  )

  val ResponsiblePeopleList = Seq(CompleteResponsiblePeople1, CompleteResponsiblePeople2, CompleteResponsiblePeople3)
}
