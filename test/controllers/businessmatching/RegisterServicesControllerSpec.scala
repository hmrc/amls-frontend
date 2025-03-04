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

package controllers.businessmatching

import cats.data.OptionT
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import generators.ResponsiblePersonGenerator
import models.businessactivities.{AccountantForAMLSRegulations, BusinessActivities, TaxMatters, WhoIsYourAccountant}
import models.businessmatching.BusinessActivity._
import models.businessmatching.{BusinessActivities => BMBusinessActivities, _}
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import models.supervision.Supervision
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.StatusService
import services.businessmatching.BusinessMatchingService
import utils._

import scala.concurrent.Future

class RegisterServicesControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with ScalaFutures
    with ResponsiblePersonGenerator {

  val activities: Set[BusinessActivity] = Set(
    AccountancyServices,
    BillPaymentServices,
    EstateAgentBusinessService,
    HighValueDealing,
    MoneyServiceBusiness,
    TrustAndCompanyServices,
    TelephonePaymentService
  )

  trait Fixture extends DependencyMocks { self =>

    val request = addToken(authRequest)

    val statusService = mockStatusService

    val businessMatchingService = mock[BusinessMatchingService]

    val activityData1: Set[BusinessActivity] = Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService)
    val activityData2: Set[BusinessActivity] = Set(HighValueDealing, MoneyServiceBusiness)

    val businessActivities1 = BMBusinessActivities(activityData1)

    val businessMatching1 = BusinessMatching(None, Some(businessActivities1))

    lazy val app = new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
      )
      .overrides(bind[BusinessMatchingService].to(businessMatchingService))
      .overrides(bind[StatusService].to(statusService))
      .overrides(bind[AuthAction].to(SuccessfulAuthAction))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .build()

    lazy val controller = app.injector.instanceOf[RegisterServicesController]

    when {
      controller.statusService.isPreSubmission(Some(any()), any(), any())(any(), any(), any())
    } thenReturn Future.successful(true)

    when {
      controller.businessMatchingService.updateModel(any(), any())(any())
    } thenReturn OptionT.liftF(Future.successful(mockCacheMap))

    when {
      controller.businessMatchingService.clearSection(any(), any())
    } thenReturn Future.successful(mockCacheMap)

    val responsiblePerson        =
      responsiblePersonGen.sample.get.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = None))
    val responsiblePersonChanged = responsiblePerson.copy(hasChanged = true, hasAccepted = true)

    val fitAndProperResponsiblePeople = Seq(
      responsiblePerson.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true))),
      responsiblePerson.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)))
    )

    mockCacheFetch[Seq[ResponsiblePerson]](Some(fitAndProperResponsiblePeople), Some(ResponsiblePerson.key))
    mockCacheSave[Seq[ResponsiblePerson]]

    mockCacheFetch[BusinessActivities](Some(BusinessActivities()), Some(BusinessActivities.key))
    mockCacheSave[BusinessActivities]

  }

  "RegisterServicesController" when {

    "get is called" must {

      "display the view" which {
        "shows empty fields" in new Fixture {

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(BusinessMatching())))

          val result = controller.get()(request)
          status(result)          must be(OK)
          contentAsString(result) must include("   ")
        }

        "populates fields" in new Fixture {

          import collection.JavaConverters._

          when {
            controller.businessMatchingService.getModel(any())
          } thenReturn OptionT.liftF(Future.successful(businessMatching1))

          val result = controller.get()(request)
          status(result) must be(OK)

          val document = Jsoup.parse(contentAsString(result))

          private val checkboxes = document.select("input[checked]")

          checkboxes.eachAttr("value").asScala.zip(activityData1).foreach { case (checkboxName, activity) =>
            checkboxName mustBe activity.toString
          }
        }
      }
    }

    "post is called" when {
      "request data is valid" must {
        "redirect to SummaryController" when {
          "edit is false" in new Fixture {

            val businessActivitiesWithData = BMBusinessActivities(businessActivities = activityData1)
            val businessMatchingWithData   = BusinessMatching(None, Some(businessActivitiesWithData))

            val newRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
              .withFormUrlEncodedBody(
                "value[1]" -> "accountancyServices",
                "value[2]" -> "billPaymentServices",
                "value[3]" -> "estateAgentBusinessService"
              )

            when(controller.businessMatchingService.getModel(any()))
              .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }

          "edit is true" in new Fixture {

            val businessActivitiesWithData = BMBusinessActivities(businessActivities = activityData2)
            val businessMatchingWithData   = BusinessMatching(None, Some(businessActivitiesWithData))

            val newRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
              .withFormUrlEncodedBody(
                "value[1]" -> "accountancyServices",
                "value[2]" -> "billPaymentServices",
                "value[3]" -> "estateAgentBusinessService"
              )

            when(controller.businessMatchingService.getModel(any()))
              .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

            val result = controller.post(true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get().url))
          }
        }

        "redirect to ServicesController" when {
          "msb is selected" in new Fixture {

            val businessActivities =
              BMBusinessActivities(businessActivities = Set(HighValueDealing, MoneyServiceBusiness))
            val bm                 = BusinessMatching(None, Some(businessActivities))

            val newRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
              .withFormUrlEncodedBody(
                "value[1]" -> "highValueDealing",
                "value[2]" -> "moneyServiceBusiness"
              )

            when(controller.businessMatchingService.getModel(any()))
              .thenReturn(OptionT.liftF(Future.successful(bm)))

            val result = controller.post(true)(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.MsbSubSectorsController.get(false).url))
          }
        }
      }

      "request data is invalid" must {
        "return BAD_REQUEST" in new Fixture {

          val businessActivitiesWithData = BMBusinessActivities(businessActivities = activityData1)
          val businessMatchingWithData   = BusinessMatching(None, Some(businessActivitiesWithData))

          val newRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "highValueDealing",
              "value[2]" -> "foo"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "remove the accountancy advisor questions from Business Activities" when {
        "Accountancy Services is added and pre-application is complete and BusinessActivities section is started" in new Fixture {
          val businessActivities = BusinessActivities(
            accountantForAMLSRegulations = Some(AccountantForAMLSRegulations(true)),
            whoIsYourAccountant = Some(mock[WhoIsYourAccountant]),
            taxMatters = Some(TaxMatters(true))
          )

          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(HighValueDealing))),
            preAppComplete = true
          )

          val newRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices",
              "value[2]" -> "highValueDealing"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          mockCacheFetch(Some(businessActivities), Some(BusinessActivities.key))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)

          val captor = ArgumentCaptor.forClass(classOf[BusinessActivities])
          verify(controller.dataCacheConnector).save(any(), eqTo(BusinessActivities.key), captor.capture())(any())

          captor.getValue.accountantForAMLSRegulations mustBe None
          captor.getValue.whoIsYourAccountant mustBe None
          captor.getValue.taxMatters mustBe None
        }
      }

      "NOT attempt to remove the accountancy advisor questions from Business Activities" when {
        "Accountancy Services is added and BusinessActivities section is not started" in new Fixture {

          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(HighValueDealing))),
            preAppComplete = false
          )

          val newRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices",
              "value[2]" -> "highValueDealing"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          mockCacheFetch[BusinessActivities](None, Some(BusinessActivities.key))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)

          verify(controller.dataCacheConnector, times(0))
            .save(any(), eqTo(BusinessActivities.key), any())(any())

        }
      }
    }

    "getActivityValues is called" must {
      "have no existing services" when {
        "status is pre-submission" when {
          "activities have not yet been selected" in new Fixture {
            controller.getActivityValues(true, None) must be(empty)
          }

          "activities have already been selected" in new Fixture {
            controller.getActivityValues(true, Some(activityData1)) must be(empty)
          }
        }
      }

      "return values for services excluding those provided" when {
        "status is post-submission" when {

          activities.foreach { act =>
            s"$act is contained in existing activities" in new Fixture {

              val activityData: Set[BusinessActivity] = Set(act)
              controller.getActivityValues(false, Some(activityData)) must be(Seq(act))
            }
          }
        }
      }
    }

    "post" must {
      "Do nothing to non-existing supervision section data" when {
        "ASP, TCSP not selected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices))),
            preAppComplete = true
          )

          val newRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())
        }

        "ASP added, TCSP not selected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())
        }

        "TCSP added, ASP not selected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "trustAndCompanyServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())
        }

        "ASP, TCSP added" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices",
              "value[2]" -> "trustAndCompanyServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())
        }
      }

      "Remove supervision section data" when {
        "ASP deselected, TCSP not selected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices, AccountancyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(1)).save(any(), eqTo(Supervision.key), any())(any())
        }

        "TCSP deselected, ASP not selected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices, TrustAndCompanyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(1)).save(any(), eqTo(Supervision.key), any())(any())

        }

        "ASP, TCSP deselected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(
              BMBusinessActivities(businessActivities =
                Set(BillPaymentServices, AccountancyServices, TrustAndCompanyServices)
              )
            ),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(1)).save(any(), eqTo(Supervision.key), any())(any())
        }

      }

      "Must not remove supervision section data" when {
        "ASP selected, TCSP not selected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices, AccountancyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices",
              "value[2]" -> "billPaymentServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())

        }

        "ASP selected, TCSP deselected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(
              BMBusinessActivities(businessActivities =
                Set(BillPaymentServices, AccountancyServices, TrustAndCompanyServices)
              )
            ),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices",
              "value[2]" -> "billPaymentServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())

        }

        "TCSP selected, ASP not selected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices, TrustAndCompanyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices",
              "value[2]" -> "trustAndCompanyServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())

        }

        "TCSP selected, ASP deselected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(
              BMBusinessActivities(businessActivities =
                Set(BillPaymentServices, AccountancyServices, TrustAndCompanyServices)
              )
            ),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices",
              "value[2]" -> "trustAndCompanyServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())
        }

        "ASP, TCSP selected" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(
              BMBusinessActivities(businessActivities =
                Set(BillPaymentServices, AccountancyServices, TrustAndCompanyServices)
              )
            ),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices",
              "value[2]" -> "billPaymentServices",
              "value[3]" -> "trustAndCompanyServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())

        }

        "ASP deselected, TCSP added" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices, AccountancyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices",
              "value[2]" -> "trustAndCompanyServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))
          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())

        }

        "TCSP deselected, ASP added" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(BillPaymentServices, TrustAndCompanyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices",
              "value[2]" -> "billPaymentServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.dataCacheConnector, times(0)).save(any(), eqTo(Supervision.key), any())(any())

        }
      }

      "Do nothing to non-existing section data" when {
        "ASP is not selected, and was not previously" in new Fixture {
          val businessMatchingWithData =
            BusinessMatching(None, Some(BMBusinessActivities(businessActivities = Set())), preAppComplete = true)
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )

          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(AccountancyServices))
        }

        "EAB is not selected, and was not previously" in new Fixture {
          val businessMatchingWithData =
            BusinessMatching(None, Some(BMBusinessActivities(businessActivities = Set())), preAppComplete = true)
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(EstateAgentBusinessService))
        }

        "HVD is not selected, and was not previously" in new Fixture {
          val businessMatchingWithData =
            BusinessMatching(None, Some(BMBusinessActivities(businessActivities = Set())), preAppComplete = true)
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(HighValueDealing))
        }

        "MSB is not selected, and was not previously" in new Fixture {
          val businessMatchingWithData =
            BusinessMatching(None, Some(BMBusinessActivities(businessActivities = Set())), preAppComplete = true)
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(MoneyServiceBusiness))
        }

        "TCSP is not selected, and was not previously" in new Fixture {
          val businessMatchingWithData =
            BusinessMatching(None, Some(BMBusinessActivities(businessActivities = Set())), preAppComplete = true)
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(TrustAndCompanyServices))
        }
      }

      "Remove existing section data " when {
        "ASP is not selected, but was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(AccountancyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(1)).clearSection(any(), eqTo(AccountancyServices))
        }

        "EAB is not selected, but was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(EstateAgentBusinessService))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "artMarketParticipant"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(1)).clearSection(any(), eqTo(EstateAgentBusinessService))
        }

        "HVD is not selected, but was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(HighValueDealing))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(1)).clearSection(any(), eqTo(HighValueDealing))
        }

        "MSB is not selected, but was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(MoneyServiceBusiness))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(1)).clearSection(any(), eqTo(MoneyServiceBusiness))
        }

        "TCSP is not selected, but was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(TrustAndCompanyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "billPaymentServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(1)).clearSection(any(), eqTo(TrustAndCompanyServices))
        }
      }

      "Must not remove existing section data " when {
        "ASP is selected and was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(AccountancyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "accountancyServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(AccountancyServices))
        }

        "EAB is selected and was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(EstateAgentBusinessService))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "estateAgentBusinessService"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(EstateAgentBusinessService))
        }

        "HVD is selected and was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(HighValueDealing))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "highValueDealing"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(HighValueDealing))
        }

        "MSB is selected and was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(MoneyServiceBusiness))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "moneyServiceBusiness"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(MoneyServiceBusiness))
        }

        "TCSP is selected and was previously" in new Fixture {
          val businessMatchingWithData = BusinessMatching(
            None,
            Some(BMBusinessActivities(businessActivities = Set(TrustAndCompanyServices))),
            preAppComplete = true
          )
          val newRequest               = FakeRequest(POST, routes.RegisterServicesController.post().url)
            .withFormUrlEncodedBody(
              "value[1]" -> "trustAndCompanyServices"
            )
          when(controller.businessMatchingService.getModel(any()))
            .thenReturn(OptionT.liftF(Future.successful(businessMatchingWithData)))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          verify(controller.businessMatchingService, times(0)).clearSection(any(), eqTo(TrustAndCompanyServices))
        }
      }
    }

    "promptFitAndProper" must {
      "return true" when {
        "a responsible person has fitAndProper not defined" in new Fixture {
          controller.promptFitAndProper(responsiblePerson) must be(true)

        }
      }
      "return false" when {
        "all responsible people have fitAndProper defined" in new Fixture {
          controller.promptFitAndProper(
            responsiblePerson.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)))
          ) must be(false)

        }
      }
    }
  }

  it must {
    "save additional services as additionalActivities" when {
      "status is post-submission" in new Fixture {

        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF(Future.successful(businessMatching1))

        when {
          controller.statusService.isPreSubmission(Some(any()), any(), any())(any(), any(), any())
        } thenReturn Future.successful(false)

        val postRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "highValueDealing",
            "value[2]" -> "telephonePaymentService"
          )

        val result = controller.post()(postRequest)

        status(result) must be(SEE_OTHER)

        verify(controller.businessMatchingService).updateModel(
          any(),
          eqTo(
            businessMatching1.activities(
              BMBusinessActivities(activityData1, Some(Set(HighValueDealing, TelephonePaymentService)))
            )
          )
        )(any())

      }
    }
    "save only services from request to businessActivties" when {
      "status is pre-submisson" in new Fixture {

        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF(Future.successful(businessMatching1))

        when {
          controller.statusService.isPreSubmission(Some(any()), any(), any())(any(), any(), any())
        } thenReturn Future.successful(true)

        val postRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "highValueDealing",
            "value[2]" -> "telephonePaymentService"
          )

        val result = controller.post()(postRequest)

        status(result) must be(SEE_OTHER)

        verify(controller.businessMatchingService).updateModel(
          any(),
          eqTo(
            businessMatching1.activities(
              BMBusinessActivities(Set(HighValueDealing, TelephonePaymentService))
            )
          )
        )(any())

      }
    }
    "set RP hasAccepted to false" when {
      "fitAndProper is required and fitAndProper is not defined" in new Fixture {

        val responsiblePersonNotAccepted = responsiblePerson.copy(hasAccepted = false)

        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF(
          Future.successful(BusinessMatching(None, Some(BMBusinessActivities(Set(HighValueDealing)))))
        )

        mockCacheFetch[Seq[ResponsiblePerson]](
          Some(Seq(responsiblePerson, responsiblePerson)),
          Some(ResponsiblePerson.key)
        )

        val postRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "trustAndCompanyServices"
          )

        val result = controller.post()(postRequest)

        status(result) must be(SEE_OTHER)

        verify(mockCacheConnector).save[Seq[ResponsiblePerson]](
          any(),
          eqTo(ResponsiblePerson.key),
          eqTo(Seq(responsiblePersonNotAccepted, responsiblePersonNotAccepted))
        )(any())

      }
    }
    "not update RP" when {
      "fitAndProper is required" in new Fixture {

        when {
          controller.businessMatchingService.getModel(any())
        } thenReturn OptionT.liftF(
          Future.successful(
            BusinessMatching(None, Some(BMBusinessActivities(Set(TrustAndCompanyServices, MoneyServiceBusiness))))
          )
        )

        val postRequest = FakeRequest(POST, routes.RegisterServicesController.post().url)
          .withFormUrlEncodedBody(
            "value[1]" -> "trustAndCompanyServices"
          )

        val result = controller.post()(postRequest)

        status(result) must be(SEE_OTHER)

        verify(mockCacheConnector).fetch[Seq[ResponsiblePerson]](any(), eqTo(ResponsiblePerson.key))(any())

      }
    }
    "shouldPromptForApproval" must {
      "reset approval flag to none" when {
        "removing a business activity and does not have MSB or TCSP and FitandProper flag has a value of false" in new Fixture {

          val rp         = responsiblePerson.copy(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = Some(false)
            )
          )
          val bm         = BMBusinessActivities(businessActivities = Set(HighValueDealing))
          val isRemoving = true

          val result = controller.shouldPromptForApproval(rp, bm, isRemoving)

          val expectedRp = responsiblePerson.copy(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = None
            ),
            hasAccepted = false,
            hasChanged = true
          )
          val expectedBm = bm

          result mustEqual ((expectedRp, expectedBm))
        }
      }

      "not handle any approvalFlags" when {
        "adding a business activity" in new Fixture {

          val rp         = responsiblePerson.copy(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = Some(false)
            )
          )
          val bm         = BMBusinessActivities(businessActivities = Set(HighValueDealing))
          val isRemoving = false

          val result = controller.shouldPromptForApproval(rp, bm, isRemoving)

          val expectedRp = rp
          val expectedBm = bm

          result mustEqual ((expectedRp, expectedBm))
        }
      }
    }
  }
}
