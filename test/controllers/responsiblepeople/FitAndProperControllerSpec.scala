/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.DataCacheConnector
import models.businessmatching._
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{ApprovalFlags, PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import org.mockito.Matchers.{eq => meq, _}
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future
import scala.runtime.Nothing$

class FitAndProperControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)

    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .configure("microservice.services.feature-toggle.show-fees" -> true)
      .configure("microservice.services.feature-toggle.phase-2-changes" -> false)
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val controller = app.injector.instanceOf[FitAndProperController]

    def setupCache(
                    approvalFlags: ApprovalFlags,
                    personName: Option[PersonName] = None): Unit = {

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
        .thenReturn(Some(Seq(ResponsiblePerson(
          personName = personName,
          approvalFlags = approvalFlags
        ))))

      when(mockCacheMap.getEntry[BusinessMatching](meq(BusinessMatching.key))(any()))
        .thenReturn(Some(BusinessMatching(
          activities = Some(BusinessActivities(Set(HighValueDealing))),
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
        )))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(mockCacheMap))
    }

  }

  val testFitAndProper = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true))

  "BusinessRegisteredForVATController" when {
    "get is called" must {
      "respond with OK" when {
        "there is a PersonName and value for hasAlreadyPassedFitAndProper present" in new Fixture {

          setupCache(
            testFitAndProper,
            Some(
              PersonName(
                firstName = "firstName",
                middleName = None,
                lastName = "lastName"
              )
            )
          )

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=true]").hasAttr("checked") must be(true)
          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=false]").hasAttr("checked") must be(false)

        }

        "there is a PersonName but has not passed fit and proper" in new Fixture {

          setupCache(
            ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false)
            ),
            Some(
              PersonName(
                firstName = "firstName",
                middleName = None,
                lastName = "lastName"
              )
            )
          )

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=true]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=false]").hasAttr("checked") must be(true)

        }

        "there is a PersonName but no value for hasAlreadyPassedFitAndProper" in new Fixture {

          setupCache(
            ApprovalFlags(hasAlreadyPassedFitAndProper = None),
            Some(
              PersonName(
                firstName = "firstName",
                middleName = None,
                lastName = "lastName"
              )
            )
          )

          val result = controller.get(1)(request)
          status(result) must be(OK)

          val document: Document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=true]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=hasAlreadyPassedFitAndProper][value=false]").hasAttr("checked") must be(false)

        }
      }

      "respond with NOT_FOUND" when {
        "there is no PersonName present" in new Fixture {
          setupCache(
            ApprovalFlags(hasAlreadyPassedFitAndProper = None),
            None
          )

          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)

        }
      }
    }

    "post is called" must {
      "respond with NOT_FOUND" when {
        "the index is out of bounds" in new Fixture {

          setupCache(testFitAndProper)

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "true"
          )

          val result = controller.post(99)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }
      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "invalid"
          )
          setupCache(testFitAndProper)

          val result = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with SEE_OTHER" when {
        "given valid data and edit = false, and redirect to the DetailedAnswersController" in new Fixture {

          setupCache(testFitAndProper)

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "true"
          )

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
        }

        "given valid data and edit = true, and redirect to the DetailedAnswersController" in new Fixture {

          setupCache(testFitAndProper)

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "true"
          )

          val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
        }
      }
    }
  }
}

class FitAndProperControllerSpecPhase2 extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)

    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .configure("microservice.services.feature-toggle.show-fees" -> true)
      .configure("microservice.services.feature-toggle.phase-2-changes" -> true)
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val controller = app.injector.instanceOf[FitAndProperController]

    def setupCache(
                    approvalFlags: ApprovalFlags,
                    personName: Option[PersonName] = None,
                    activities: Option[BusinessActivities]): Unit = {

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
        .thenReturn(Some(Seq(ResponsiblePerson(
          personName = personName,
          approvalFlags = approvalFlags
        ))))

      when(mockCacheMap.getEntry[BusinessMatching](meq(BusinessMatching.key))(any()))
        .thenReturn(Some(BusinessMatching(
          activities = activities,
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney)))
        )))

      when(controller.dataCacheConnector.fetchAll(any(), any()))
        .thenReturn(Future.successful(Some(mockCacheMap)))

      when(controller.dataCacheConnector.save(any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(mockCacheMap))
    }

  }

  val testFitAndProper = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true))

  "BusinessRegisteredForVATController" when {
    "post is called" must {
      "respond with NOT_FOUND" when {
        "the index is out of bounds" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "true"
          )

          setupCache(testFitAndProper, Some(
                        PersonName(
                          firstName = "firstName",
                          middleName = None,
                          lastName = "lastName"
                        )
                      ), Some(BusinessActivities(Set(HighValueDealing))))

          val result = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
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

          val newRequest = request.withFormUrlEncodedBody(
            "hasAlreadyPassedFitAndProper" -> "true"
          )

          setupCache(
            testFitAndProper,
            activities = Some(BusinessActivities(Set(HighValueDealing)))
          )

          val result = controller.post(1)(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url))
        }

        "updates approval flags correctly" when {

        }

        "routes correctly" when {
          "given edit = true" when {
            "given fit and proper true, and redirect to the DetailedAnswersController" in new Fixture {
              val newRequest = request.withFormUrlEncodedBody(
                "hasAlreadyPassedFitAndProper" -> "true"
              )

              setupCache(
                testFitAndProper,
                activities = Some(BusinessActivities(Set(HighValueDealing)))
              )

              val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
            }

            "given fit and proper false" when {
              "given some business matching" when {
                "given TrustAndCompanyServices, and redirect to the DetailedAnswersController" in new Fixture {
                  val newRequest = request.withFormUrlEncodedBody(
                    "hasAlreadyPassedFitAndProper" -> "false"
                  )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(TrustAndCompanyServices,HighValueDealing)))
                  )

                  val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
                  status(result) must be(SEE_OTHER)
                  redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
                }

                "given MoneyServiceBusiness, and redirect to the DetailedAnswersController" in new Fixture {
                  val newRequest = request.withFormUrlEncodedBody(
                    "hasAlreadyPassedFitAndProper" -> "false"
                  )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(MoneyServiceBusiness,HighValueDealing)))
                  )

                  val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
                  status(result) must be(SEE_OTHER)
                  redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
                }

                "given NOT TrustAndCompanyServices or MoneyServiceBusiness, and redirect to the ApprovalCheckController" in new Fixture {
                  val newRequest = request.withFormUrlEncodedBody(
                    "hasAlreadyPassedFitAndProper" -> "false"
                  )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(HighValueDealing)))
                  )

                  val result = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
                  status(result) must be(SEE_OTHER)
                  redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ApprovalCheckController.get(1, false, Some(flowFromDeclaration)).url))
                }
              }
            }
          }

          "given edit = false" when {
            "given fit and proper true, and redirect to the DetailedAnswersController" in new Fixture {
              val newRequest = request.withFormUrlEncodedBody(
                "hasAlreadyPassedFitAndProper" -> "true"
              )

              setupCache(
                testFitAndProper,
                activities = Some(BusinessActivities(Set(HighValueDealing)))
              )

              val result = controller.post(1, false, Some(flowFromDeclaration))(newRequest)
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
            }

            "given fit and proper false" when {
              "given some business matching" when {
                "given TrustAndCompanyServices, and redirect to the DetailedAnswersController" in new Fixture {
                  val newRequest = request.withFormUrlEncodedBody(
                    "hasAlreadyPassedFitAndProper" -> "false"
                  )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(TrustAndCompanyServices,HighValueDealing)))
                  )

                  val result = controller.post(1, false, Some(flowFromDeclaration))(newRequest)
                  status(result) must be(SEE_OTHER)
                  redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
                }

                "given MoneyServiceBusiness, and redirect to the DetailedAnswersController" in new Fixture {
                  val newRequest = request.withFormUrlEncodedBody(
                    "hasAlreadyPassedFitAndProper" -> "false"
                  )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(MoneyServiceBusiness,HighValueDealing)))
                  )

                  val result = controller.post(1, false, Some(flowFromDeclaration))(newRequest)
                  status(result) must be(SEE_OTHER)
                  redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url))
                }

                "given NOT TrustAndCompanyServices or MoneyServiceBusiness, and redirect to the ApprovalCheckController" in new Fixture {
                  val newRequest = request.withFormUrlEncodedBody(
                    "hasAlreadyPassedFitAndProper" -> "false"
                  )

                  setupCache(
                    testFitAndProper,
                    activities = Some(BusinessActivities(Set(HighValueDealing)))
                  )

                  val result = controller.post(1, false, Some(flowFromDeclaration))(newRequest)
                  status(result) must be(SEE_OTHER)
                  redirectLocation(result) must be(Some(controllers.responsiblepeople.routes.ApprovalCheckController.get(1, false, Some(flowFromDeclaration)).url))
                }
              }
            }
          }
        }
      }
    }
  }
}
