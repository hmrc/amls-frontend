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
import forms.responsiblepeople.ApprovalCheckFormProvider
import models.responsiblepeople.ResponsiblePerson._
import models.responsiblepeople.{ApprovalFlags, PersonName, ResponsiblePerson}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.responsiblepeople.ApprovalCheckView

class ApprovalCheckControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with Injecting {
  trait Fixture extends DependencyMocks { self =>
    val request = addToken(authRequest)

    lazy val controller = new ApprovalCheckController(
      mockCacheConnector,
      SuccessfulAuthAction,
      commonDependencies,
      mockMcc,
      inject[ApprovalCheckFormProvider],
      inject[ApprovalCheckView],
      errorView
    )
  }

  val testApproval = ApprovalFlags(hasAlreadyPaidApprovalCheck = Some(true))

  "ApprovalCheckController" when {
    "get is called"  must {
      "respond with OK" when {
        "there is a PersonName and value for hasAlreadyPaidApprovalCheck present" in new Fixture {
          mockCacheFetch[Seq[ResponsiblePerson]](
            Some(
              Seq(
                ResponsiblePerson(
                  personName = Some(PersonName("firstName", None, "lastName")),
                  approvalFlags = testApproval
                )
              )
            ),
            Some(ResponsiblePerson.key)
          )
          val result = controller.get(1)(request)
          status(result) must be(OK)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("input[type=radio][name=hasAlreadyPaidApprovalCheck][value=true]").hasAttr("checked") must be(
            true
          )
          document
            .select("input[type=radio][name=hasAlreadyPaidApprovalCheck][value=false]")
            .hasAttr("checked")                                                                                 must be(false)
        }
        "there is a PersonName but has not paid approval" in new Fixture {
          mockCacheFetch[Seq[ResponsiblePerson]](
            Some(
              Seq(
                ResponsiblePerson(
                  personName = Some(PersonName("firstName", None, "lastName")),
                  approvalFlags = ApprovalFlags(hasAlreadyPaidApprovalCheck = Some(false))
                )
              )
            ),
            Some(ResponsiblePerson.key)
          )
          val result = controller.get(1)(request)
          status(result) must be(OK)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("input[type=radio][name=hasAlreadyPaidApprovalCheck][value=true]").hasAttr("checked") must be(
            false
          )
          document
            .select("input[type=radio][name=hasAlreadyPaidApprovalCheck][value=false]")
            .hasAttr("checked")                                                                                 must be(true)
        }
        "there is a PersonName but no value for hasAlreadyPaidApprovalCheck" in new Fixture {
          mockCacheFetch[Seq[ResponsiblePerson]](
            Some(
              Seq(
                ResponsiblePerson(
                  personName = Some(PersonName("firstName", None, "lastName")),
                  approvalFlags = ApprovalFlags(hasAlreadyPaidApprovalCheck = None)
                )
              )
            ),
            Some(ResponsiblePerson.key)
          )
          val result = controller.get(1)(request)
          status(result) must be(OK)
          val document: Document = Jsoup.parse(contentAsString(result))
          document.select("input[type=radio][name=hasAlreadyPaidApprovalCheck][value=true]").hasAttr("checked") must be(
            false
          )
          document
            .select("input[type=radio][name=hasAlreadyPaidApprovalCheck][value=false]")
            .hasAttr("checked")                                                                                 must be(false)
        }
      }
      "respond with NOT_FOUND" when {
        "there is no PersonName present" in new Fixture {
          mockCacheFetch[Seq[ResponsiblePerson]](
            Some(
              Seq(
                ResponsiblePerson(
                  personName = None,
                  approvalFlags = ApprovalFlags(hasAlreadyPaidApprovalCheck = None)
                )
              )
            ),
            Some(ResponsiblePerson.key)
          )
          val result = controller.get(1)(request)
          status(result) must be(NOT_FOUND)
        }
      }
    }
    "post is called" must {
      "respond with NOT_FOUND" when {
        "the index is out of bounds" in new Fixture {
          val newRequest = FakeRequest(POST, routes.ApprovalCheckController.post(1).url)
            .withFormUrlEncodedBody(
              "hasAlreadyPaidApprovalCheck" -> "true"
            )
          mockCacheFetch[Seq[ResponsiblePerson]](
            Some(
              Seq(
                ResponsiblePerson(
                  approvalFlags = testApproval
                )
              )
            ),
            Some(ResponsiblePerson.key)
          )
          mockCacheSave[Seq[ResponsiblePerson]]
          val result     = controller.post(99)(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }
      "respond with BAD_REQUEST" when {
        "given invalid data" in new Fixture {
          val newRequest = FakeRequest(POST, routes.ApprovalCheckController.post(1).url)
            .withFormUrlEncodedBody(
              "hasAlreadyPaidApprovalCheck" -> "invalid"
            )
          mockCacheFetch[Seq[ResponsiblePerson]](
            Some(
              Seq(
                ResponsiblePerson(
                  approvalFlags = testApproval
                )
              )
            ),
            Some(ResponsiblePerson.key)
          )
          val result     = controller.post(1)(newRequest)
          status(result) must be(BAD_REQUEST)
        }
      }
      "respond with SEE_OTHER" when {
        "given valid data and edit = false, and redirect to the DetailedAnswersController" in new Fixture {
          val newRequest = FakeRequest(POST, routes.ApprovalCheckController.post(1).url)
            .withFormUrlEncodedBody(
              "hasAlreadyPaidApprovalCheck" -> "true"
            )
          mockCacheFetch[Seq[ResponsiblePerson]](
            Some(
              Seq(
                ResponsiblePerson(
                  approvalFlags = testApproval
                )
              )
            ),
            Some(ResponsiblePerson.key)
          )
          mockCacheSave[Seq[ResponsiblePerson]]
          val result     = controller.post(1)(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url)
          )
        }
        "given valid data and edit = true, and redirect to the DetailedAnswersController" in new Fixture {
          val newRequest = FakeRequest(POST, routes.ApprovalCheckController.post(1).url)
            .withFormUrlEncodedBody(
              "hasAlreadyPaidApprovalCheck" -> "true"
            )
          mockCacheFetch[Seq[ResponsiblePerson]](
            Some(
              Seq(
                ResponsiblePerson(
                  approvalFlags = testApproval
                )
              )
            ),
            Some(ResponsiblePerson.key)
          )
          mockCacheSave[Seq[ResponsiblePerson]]
          val result     = controller.post(1, true, Some(flowFromDeclaration))(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.responsiblepeople.routes.DetailedAnswersController.get(1, Some(flowFromDeclaration)).url)
          )
        }
      }
    }
  }
}
