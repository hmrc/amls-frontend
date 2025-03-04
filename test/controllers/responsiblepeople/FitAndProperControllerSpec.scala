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

package controllers.responsiblepeople

import controllers.actions.SuccessfulAuthAction
import forms.responsiblepeople.FitAndProperFormProvider
import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.businessmatching._
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{ApprovalFlags, PersonName, ResponsiblePerson}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils._
import views.html.responsiblepeople.FitAndProperView

import scala.concurrent.Future

class FitAndProperControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {

  trait Fixture extends DependencyMocks { self =>
    val request = addToken(authRequest)

    lazy val controller = new FitAndProperController(
      mockCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      inject[FitAndProperFormProvider],
      inject[FitAndProperView],
      errorView
    )

    def setupCache(
      approvalFlags: ApprovalFlags,
      personName: Option[PersonName] = None,
      activities: Option[BusinessActivities]
    ): Unit = {

      mockCacheFetch[Seq[ResponsiblePerson]](
        item = Some(
          Seq(
            ResponsiblePerson(
              personName = personName,
              approvalFlags = approvalFlags
            )
          )
        ),
        key = Some(ResponsiblePerson.key)
      )

      when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](any())(any()))
        .thenReturn(
          Some(
            Seq(
              ResponsiblePerson(
                personName = personName,
                approvalFlags = approvalFlags
              )
            )
          )
        )

      when(mockCacheMap.getEntry[BusinessMatching](meq(BusinessMatching.key))(any()))
        .thenReturn(
          Some(
            BusinessMatching(
              activities = activities,
              msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
            )
          )
        )

      when(controller.dataCacheConnector.fetchAll(any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any(), any())(any()))
        .thenReturn(Future.successful(mockCacheMap))
    }

  }

  val testFitAndProper = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true))

  "BusinessRegisteredForVATController" when {
    "post is called" must {
      "respond with NOT_FOUND" when {
        "the index is out of bounds" in new Fixture {

          val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
            .withFormUrlEncodedBody(
              "hasAlreadyPassedFitAndProper" -> "true"
            )

          setupCache(
            testFitAndProper,
            Some(
              PersonName(
                firstName = "firstName",
                middleName = None,
                lastName = "lastName"
              )
            ),
            Some(BusinessActivities(Set(HighValueDealing)))
          )

          val result = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
            .withFormUrlEncodedBody(
              "hasAlreadyPassedFitAndProper" -> "invalid"
            )

          setupCache(
            testFitAndProper,
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with SEE_OTHER" when {
        "given valid data and edit = false, and redirect to the DetailedAnswersController" in new Fixture {

          val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
            .withFormUrlEncodedBody(
              "hasAlreadyPassedFitAndProper" -> "true"
            )

          setupCache(
            testFitAndProper,
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )

          val result = controller.post(1)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url)
          )
        }

        "routes correctly" when {
          "given edit = true" when {
            "given fit and proper true, and redirect to the DetailedAnswersController" in new Fixture {
              val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
                .withFormUrlEncodedBody(
                  "hasAlreadyPassedFitAndProper" -> "true"
                )

              setupCache(
                testFitAndProper,
                activities = Some(BusinessActivities(Set(HighValueDealing)))
              )

              val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
              status(result)           must be(SEE_OTHER)
              redirectLocation(result) must be(
                Some(
                  controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url
                )
              )
            }

            "given fit and proper false" when {
              "given some business matching" when {
                "given TrustAndCompanyServices, and redirect to the DetailedAnswersController" in new Fixture {
                  val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
                    .withFormUrlEncodedBody(
                      "hasAlreadyPassedFitAndProper" -> "false"
                    )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(TrustAndCompanyServices, HighValueDealing)))
                  )

                  val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
                  status(result)           must be(SEE_OTHER)
                  redirectLocation(result) must be(
                    Some(
                      controllers.responsiblepeople.routes.DetailedAnswersController
                        .get(1, Some(flowFromDeclaration))
                        .url
                    )
                  )
                }

                "given MoneyServiceBusiness, and redirect to the DetailedAnswersController" in new Fixture {
                  val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
                    .withFormUrlEncodedBody(
                      "hasAlreadyPassedFitAndProper" -> "false"
                    )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(MoneyServiceBusiness, HighValueDealing)))
                  )

                  val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
                  status(result)           must be(SEE_OTHER)
                  redirectLocation(result) must be(
                    Some(
                      controllers.responsiblepeople.routes.DetailedAnswersController
                        .get(1, Some(flowFromDeclaration))
                        .url
                    )
                  )
                }

                "given NOT TrustAndCompanyServices or MoneyServiceBusiness, and redirect to the ApprovalCheckController" in new Fixture {
                  val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
                    .withFormUrlEncodedBody(
                      "hasAlreadyPassedFitAndProper" -> "false"
                    )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(HighValueDealing)))
                  )

                  val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
                  status(result)           must be(SEE_OTHER)
                  redirectLocation(result) must be(
                    Some(
                      controllers.responsiblepeople.routes.ApprovalCheckController
                        .get(1, false, Some(flowFromDeclaration))
                        .url
                    )
                  )
                }
              }
            }
          }

          "given edit = false" when {
            "given fit and proper true, and redirect to the DetailedAnswersController" in new Fixture {
              val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
                .withFormUrlEncodedBody(
                  "hasAlreadyPassedFitAndProper" -> "true"
                )

              setupCache(
                testFitAndProper,
                activities = Some(BusinessActivities(Set(HighValueDealing)))
              )

              val result = controller.post(1, false, Some(flowFromDeclaration))(newRequest)
              status(result)           must be(SEE_OTHER)
              redirectLocation(result) must be(
                Some(
                  controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url
                )
              )
            }

            "given fit and proper false" when {
              "given some business matching" when {
                "given TrustAndCompanyServices, and redirect to the DetailedAnswersController" in new Fixture {
                  val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
                    .withFormUrlEncodedBody(
                      "hasAlreadyPassedFitAndProper" -> "false"
                    )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(TrustAndCompanyServices, HighValueDealing)))
                  )

                  val result = controller.post(1, false, Some(flowFromDeclaration))(newRequest)
                  status(result)           must be(SEE_OTHER)
                  redirectLocation(result) must be(
                    Some(
                      controllers.responsiblepeople.routes.DetailedAnswersController
                        .get(1, Some(flowFromDeclaration))
                        .url
                    )
                  )
                }

                "given MoneyServiceBusiness, and redirect to the DetailedAnswersController" in new Fixture {
                  val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
                    .withFormUrlEncodedBody(
                      "hasAlreadyPassedFitAndProper" -> "false"
                    )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(MoneyServiceBusiness, HighValueDealing)))
                  )

                  val result = controller.post(1, false, Some(flowFromDeclaration))(newRequest)
                  status(result)           must be(SEE_OTHER)
                  redirectLocation(result) must be(
                    Some(
                      controllers.responsiblepeople.routes.DetailedAnswersController
                        .get(1, Some(flowFromDeclaration))
                        .url
                    )
                  )
                }

                "given NOT TrustAndCompanyServices or MoneyServiceBusiness, and redirect to the ApprovalCheckController" in new Fixture {
                  val newRequest = FakeRequest(POST, routes.FitAndProperController.post(1).url)
                    .withFormUrlEncodedBody(
                      "hasAlreadyPassedFitAndProper" -> "false"
                    )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(HighValueDealing)))
                  )

                  val result = controller.post(1, false, Some(flowFromDeclaration))(newRequest)
                  status(result)           must be(SEE_OTHER)
                  redirectLocation(result) must be(
                    Some(
                      controllers.responsiblepeople.routes.ApprovalCheckController
                        .get(1, false, Some(flowFromDeclaration))
                        .url
                    )
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
