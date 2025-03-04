/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.net.URLEncoder
import config.ApplicationConfig
import connectors.DataCacheConnector
import controllers.actions.{SuccessfulAuthActionNoAmlsRefNo, SuccessfulAuthActionNoUserRole}
import generators.StatusGenerator
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.BusinessDetails
import models.businessmatching._
import models.eab.Eab
import models.responsiblepeople.TimeAtAddress.OneToThreeYears
import models.responsiblepeople._
import models.status._
import models.{status => _, _}
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import play.api.mvc.{BodyParsers, Result}
import play.api.test.Helpers._
import services.{AuthEnrolmentsService, LandingService, StatusService}
import services.cache.Cache
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.partials.HeaderCarrierForPartialsConverter
import utils.AmlsSpec
import views.html.Start

import java.time.LocalDate
import scala.concurrent.Future

class LandingControllerWithoutAmendmentsSpec extends AmlsSpec with StatusGenerator {

  trait Fixture {
    self =>

    val mockApplicationConfig = mock[ApplicationConfig]

    val request                                = addToken(authRequest)
    val config                                 = mock[ApplicationConfig]
    lazy val view                              = app.injector.instanceOf[Start]
    lazy val headerCarrierForPartialsConverter = app.injector.instanceOf[HeaderCarrierForPartialsConverter]
    val controllerNoAmlsNumber                 = new LandingController(
      enrolmentsService = mock[AuthEnrolmentsService],
      landingService = mock[LandingService],
      authAction = SuccessfulAuthActionNoAmlsRefNo,
      auditConnector = mock[AuditConnector],
      cacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      ds = commonDependencies,
      mcc = mockMcc,
      messagesApi = messagesApi,
      config = config,
      parser = mock[BodyParsers.Default],
      start = view,
      headerCarrierForPartialsConverter = headerCarrierForPartialsConverter,
      applicationCrypto = applicationCrypto
    )

    val controllerNoUserRole = new LandingController(
      enrolmentsService = mock[AuthEnrolmentsService],
      landingService = mock[LandingService],
      authAction = SuccessfulAuthActionNoUserRole,
      auditConnector = mock[AuditConnector],
      cacheConnector = mock[DataCacheConnector],
      statusService = mock[StatusService],
      ds = commonDependencies,
      mcc = mockMcc,
      messagesApi = messagesApi,
      config = config,
      parser = mock[BodyParsers.Default],
      start = view,
      headerCarrierForPartialsConverter = mock[HeaderCarrierForPartialsConverter],
      applicationCrypto = applicationCrypto
    )

    when {
      controllerNoAmlsNumber.landingService.setAltCorrespondenceAddress(any(), any[String])
    } thenReturn Future.successful(mock[Cache])

    val completeATB                                  = mock[BusinessDetails]
    val completeResponsiblePerson: ResponsiblePerson = ResponsiblePerson(
      personName = Some(PersonName("ANSTY", Some("EMIDLLE"), "DAVID")),
      legalName = Some(PreviousName(Some(false), None, None, None)),
      legalNameChangeDate = None,
      knownBy = Some(KnownBy(Some(false), None)),
      personResidenceType = Some(
        PersonResidenceType(
          NonUKResidence,
          Some(Country("Antigua and Barbuda", "bb")),
          Some(Country("United Kingdom", "GB"))
        )
      ),
      ukPassport = Some(UKPassportNo),
      nonUKPassport = Some(NoPassport),
      dateOfBirth = Some(DateOfBirth(LocalDate.of(2000, 1, 1))),
      contactDetails = Some(ContactDetails("0912345678", "TEST@EMAIL.COM")),
      addressHistory = Some(
        ResponsiblePersonAddressHistory(
          Some(
            ResponsiblePersonCurrentAddress(
              PersonAddressUK("add1", Some("add2"), Some("add3"), Some("add4"), "de4 5tg"),
              Some(OneToThreeYears),
              None
            )
          ),
          None,
          None
        )
      ),
      positions =
        Some(Positions(Set(NominatedOfficer, SoleProprietor), Some(PositionStartDate(LocalDate.of(2002, 2, 2))))),
      saRegistered = Some(SaRegisteredNo),
      vatRegistered = Some(VATRegisteredNo),
      experienceTraining = Some(ExperienceTrainingNo),
      training = Some(TrainingNo),
      approvalFlags = ApprovalFlags(Some(true), Some(true)),
      hasChanged = false,
      hasAccepted = true,
      lineId = Some(2),
      status = None,
      endDate = None,
      soleProprietorOfAnotherBusiness = None
    )
  }

  "LandingController" must {

    "redirect to status page" when {
      "submission status is DeRegistered and responsible person is not complete" in new Fixture {
        val inCompleteResponsiblePeople: ResponsiblePerson = completeResponsiblePerson.copy(
          dateOfBirth = None
        )
        val cacheMap: Cache                                = mock[Cache]
        val complete: BusinessMatching                     = mock[BusinessMatching]

        when(complete.isCompleteLanding) thenReturn true
        when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
        when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
        when(cacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
          .thenReturn(Some(Seq(inCompleteResponsiblePeople)))
        when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
          .thenReturn(
            Some(SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0))))
          )

        when(controllerNoAmlsNumber.landingService.cacheMap(any[String])) thenReturn Future.successful(Some(cacheMap))
        when(
          controllerNoAmlsNumber.statusService
            .getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any())
        )
          .thenReturn(Future.successful((rejectedStatusGen.sample.get, None)))

        val result: Future[Result] = controllerNoAmlsNumber.get()(request)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
      }
    }

    "load the correct view after calling get" when {

      "the landing service has a saved form and " when {
        "the form has not been submitted" in new Fixture {
          when(
            controllerNoAmlsNumber.statusService
              .getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any())
          )
            .thenReturn(Future.successful((NotCompleted, None)))

          val complete      = mock[BusinessMatching]
          val emptyCacheMap = mock[Cache]

          when(controllerNoAmlsNumber.landingService.cacheMap(any[String])) thenReturn Future.successful(
            Some(emptyCacheMap)
          )
          when(complete.isCompleteLanding) thenReturn true
          when(emptyCacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
          when(emptyCacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
          when(emptyCacheMap.getEntry[Eab](meq(Eab.key))(any())).thenReturn(None)

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
        }

        "the form has been submitted" in new Fixture {
          val cacheMap = mock[Cache]

          val complete = mock[BusinessMatching]

          when(complete.isCompleteLanding) thenReturn true
          when(cacheMap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))
          when(cacheMap.getEntry[BusinessDetails](BusinessDetails.key)).thenReturn(Some(completeATB))
          when(cacheMap.getEntry[Eab](meq(Eab.key))(any())).thenReturn(None)
          when(cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
            .thenReturn(
              Some(
                SubscriptionResponse("", "", Some(SubscriptionFees("", 1.0, None, None, None, None, 1.0, None, 1.0)))
              )
            )
          when(controllerNoAmlsNumber.landingService.cacheMap(any[String])) thenReturn Future.successful(Some(cacheMap))
          when(
            controllerNoAmlsNumber.statusService
              .getDetailedStatus(any(), any[(String, String)], any())(any[HeaderCarrier](), any(), any())
          )
            .thenReturn(Future.successful((SubmissionReady, None)))

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.routes.StatusController.get().url)
        }
      }

      "redirect to the sign-out page when the user role is not USER" in new Fixture {
        val expectedLocation =
          s"${appConfig.logoutUrl}?continue=${URLEncoder.encode(ReturnLocation(controllers.routes.AmlsController.unauthorised_role)(appConfig).absoluteUrl, "utf-8")}"

        val result = controllerNoUserRole.get()(request)
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(expectedLocation)
      }

      "the landing service has no saved form and " when {

        "the landing service has valid review details with UK postcode" in new Fixture {

          val details = Some(
            ReviewDetails(
              businessName = "Test",
              businessType = None,
              businessAddress =
                Address("Line 1", Some("Line 2"), None, None, Some("AA11AA"), Country("United Kingdom", "GB")),
              safeId = ""
            )
          )

          when(controllerNoAmlsNumber.landingService.cacheMap(any[String])) thenReturn Future.successful(None)
          when(controllerNoAmlsNumber.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(details))
          when(controllerNoAmlsNumber.landingService.updateReviewDetails(any(), any[String]()))
            .thenReturn(Future.successful(mock[Cache]))

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.BusinessTypeController.get().url)

          verify(controllerNoAmlsNumber.auditConnector).sendExtendedEvent(any[ExtendedDataEvent])(any(), any())
        }

        "the landing service has review details with invalid UK postcode" in new Fixture {

          val details = Some(
            ReviewDetails(
              businessName = "Test",
              businessType = None,
              businessAddress =
                Address("Line 1", Some("Line 2"), None, None, Some("aa1 $ aa156"), Country("United Kingdom", "GB")),
              safeId = ""
            )
          )

          when(controllerNoAmlsNumber.landingService.cacheMap(any[String]())) thenReturn Future.successful(None)
          when(controllerNoAmlsNumber.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(details))
          when(controllerNoAmlsNumber.landingService.updateReviewDetails(any(), any[String]()))
            .thenReturn(Future.successful(mock[Cache]))

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.ConfirmPostCodeController.get().url)
        }

        "the landing service has review details without UK postcode" in new Fixture {

          val details = Some(
            ReviewDetails(
              businessName = "Test",
              businessType = None,
              businessAddress = Address("Line 1", Some("Line 2"), None, None, None, Country("United Kingdom", "GB")),
              safeId = ""
            )
          )

          when(controllerNoAmlsNumber.landingService.cacheMap(any[String]())) thenReturn Future.successful(None)
          when(controllerNoAmlsNumber.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(details))
          when(controllerNoAmlsNumber.landingService.updateReviewDetails(any(), any[String]()))
            .thenReturn(Future.successful(mock[Cache]))

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.ConfirmPostCodeController.get().url)
        }

        "the landing service has review details without postcode but other country" in new Fixture {

          val details = Some(
            ReviewDetails(
              businessName = "Test",
              businessType = None,
              businessAddress = Address("Line 1", Some("Line 2"), None, None, None, Country("USA", "US")),
              safeId = ""
            )
          )

          when(controllerNoAmlsNumber.landingService.cacheMap(any[String]())) thenReturn Future.successful(None)
          when(controllerNoAmlsNumber.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(details))
          when(controllerNoAmlsNumber.landingService.updateReviewDetails(any(), any[String]()))
            .thenReturn(Future.successful(mock[Cache]))

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.BusinessTypeController.get().url)
        }

        "the landing service has review details without postcode and country" in new Fixture {

          val details = Some(
            ReviewDetails(
              businessName = "Test",
              businessType = None,
              businessAddress = Address("Line 1", Some("Line 2"), None, None, None, Country("", "")),
              safeId = ""
            )
          )

          when(controllerNoAmlsNumber.landingService.cacheMap(any[String]())) thenReturn Future.successful(None)
          when(controllerNoAmlsNumber.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(details))
          when(controllerNoAmlsNumber.landingService.updateReviewDetails(any(), any[String]()))
            .thenReturn(Future.successful(mock[Cache]))

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(controllers.businessmatching.routes.ConfirmPostCodeController.get().url)
        }

        "the landing service has no valid review details" in new Fixture {
          when(controllerNoAmlsNumber.landingService.cacheMap(any[String]())) thenReturn Future.successful(None)
          when(controllerNoAmlsNumber.landingService.reviewDetails(any(), any())).thenReturn(Future.successful(None))

          val result = controllerNoAmlsNumber.get()(request)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) mustBe Some(appConfig.businessCustomerUrl)
        }
      }

      "pre application must throw an exception" when {
        "the business matching is incomplete" in new Fixture {
          val cachmap      = mock[Cache]
          val httpResponse = mock[HttpResponse]

          val complete = mock[BusinessMatching]

          when(httpResponse.status) thenReturn BAD_REQUEST
          when(controllerNoAmlsNumber.landingService.cacheMap(any[String]())) thenReturn Future.successful(
            Some(cachmap)
          )
          when(complete.isCompleteLanding) thenReturn false
          when(cachmap.getEntry[BusinessMatching](any())(any())).thenReturn(Some(complete))

          a[Exception] must be thrownBy {
            await(controllerNoAmlsNumber.get()(request))
          }
        }
      }
    }
  }
}
