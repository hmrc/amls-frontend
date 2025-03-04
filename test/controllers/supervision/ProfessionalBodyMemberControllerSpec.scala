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

package controllers.supervision

import controllers.actions.SuccessfulAuthAction
import forms.supervision.MemberOfProfessionalBodyFormProvider
import models.supervision.ProfessionalBodies._
import models.supervision._
import org.jsoup.Jsoup
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import utils.{AmlsSpec, DependencyMocks}
import views.html.supervision.MemberOfProfessionalBodyView

import scala.language.postfixOps
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ProfessionalBodyMemberControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks { self =>
    val request    = addToken(authRequest)
    lazy val view  = inject[MemberOfProfessionalBodyView]
    val controller = new ProfessionalBodyMemberController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      formProvider = inject[MemberOfProfessionalBodyFormProvider],
      view = view
    )

    mockCacheSave[Supervision]

  }

  "ProfessionalBodyMemberController" must {

    "update the the answer field in supervision when the answer is yes" in new Fixture {

      val supervision = Some(
        Supervision(
          anotherBody = Some(AnotherBodyNo),
          professionalBodyMember = Some(ProfessionalBodyMemberNo),
          professionalBodies = None,
          professionalBody = Some(ProfessionalBodyNo),
          hasChanged = true,
          hasAccepted = true
        )
      )

      val newSupervision = controller.updateSupervisionFromIncomingData(ProfessionalBodyMemberYes, supervision)

      Await.result(newSupervision, 1 seconds).professionalBodyMember.get mustEqual ProfessionalBodyMemberYes
    }

    "reset professionalBodies to null if the answer to the question is No" in new Fixture {

      val supervision = Some(
        Supervision(
          anotherBody = Some(AnotherBodyNo),
          professionalBodyMember = Some(ProfessionalBodyMemberYes),
          professionalBodies = Some(ProfessionalBodies(Set(AccountingTechnicians))),
          professionalBody = Some(ProfessionalBodyNo),
          hasChanged = true,
          hasAccepted = true
        )
      )

      val newSupervision = controller.updateSupervisionFromIncomingData(ProfessionalBodyMemberNo, supervision)

      Await.result(newSupervision, 1 seconds).professionalBodies mustEqual None
    }

    "load the page Is your business a member of a professional body?" in new Fixture {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(Messages("supervision.memberofprofessionalbody.title"))

    }

    "load the page Is your business a member of a professional body? with pre-populate data" in new Fixture {

      mockCacheFetch[Supervision](
        Some(
          Supervision(
            professionalBodyMember = Some(ProfessionalBodyMemberYes)
          )
        )
      )

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=true]").hasAttr("checked") must be(true)

    }

    "on post with valid data" must {

      "redirect to WhichProfessionalBodyController" when {

        "the answer to the question is yes and no previous professional bodies exist in the cache" in new Fixture {
          val supervision = Some(
            Supervision(
              professionalBodyMember = Some(ProfessionalBodyMemberYes),
              professionalBodies = None
            )
          )

          val result: Future[Result] = Future(controller.redirectTo(supervision, false))

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.WhichProfessionalBodyController.get().url))
        }

        "isAMember field is true" when {
          "edit is false" in new Fixture {

            val newRequest = FakeRequest(POST, routes.ProfessionalBodyMemberController.post().url)
              .withFormUrlEncodedBody(
                "isAMember" -> "true"
              )

            mockCacheFetch[Supervision](None)

            val result = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.WhichProfessionalBodyController.get().url))
          }
          "edit is true" when {
            "professionalBodies is not defined" in new Fixture {

              val newRequest = FakeRequest(POST, routes.ProfessionalBodyMemberController.post().url)
                .withFormUrlEncodedBody(
                  "isAMember" -> "true"
                )

              mockCacheFetch[Supervision](None)

              val result = controller.post(true)(newRequest)
              status(result)           must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.WhichProfessionalBodyController.get(true).url))
            }
          }
        }
      }

      "redirect to PenalisedByProfessionalController" when {

        "the answer to the question is no and no previous professional bodies exist in the cache" in new Fixture {
          val supervision = Some(
            Supervision(
              professionalBodyMember = Some(ProfessionalBodyMemberNo)
            )
          )

          val result: Future[Result] = Future(controller.redirectTo(supervision, edit = false))

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.PenalisedByProfessionalController.get().url))
        }

        "isMember is false" in new Fixture {

          val newRequest = FakeRequest(POST, routes.ProfessionalBodyMemberController.post().url)
            .withFormUrlEncodedBody(
              "isAMember" -> "false"
            )

          mockCacheFetch[Supervision](None)

          val result = controller.post()(newRequest)
          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.PenalisedByProfessionalController.get().url))
        }
      }

      "redirect to SummaryController" when {

        "the answer is yes and professional bodies is already known" in new Fixture {
          val supervision = Some(
            Supervision(
              anotherBody = Some(AnotherBodyNo),
              professionalBodyMember = Some(ProfessionalBodyMemberYes),
              professionalBodies = Some(ProfessionalBodies(Set(AccountingTechnicians))),
              professionalBody = Some(ProfessionalBodyNo),
              hasChanged = true,
              hasAccepted = true
            )
          )

          val result: Future[Result] = Future(controller.redirectTo(supervision, false))

          status(result)           must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.SummaryController.get().url))

        }

        "edit is true" when {
          "isMember is true" when {
            "ProfessionalBodyMemberYes is already defined and professional bodies provided" in new Fixture {

              val newRequest = FakeRequest(POST, routes.ProfessionalBodyMemberController.post().url)
                .withFormUrlEncodedBody(
                  "isAMember" -> "true"
                )

              mockCacheFetch[Supervision](
                Some(
                  Supervision(
                    anotherBody = Some(AnotherBodyNo),
                    professionalBodyMember = Some(ProfessionalBodyMemberYes),
                    professionalBodies = Some(ProfessionalBodies(Set(AccountingTechnicians))),
                    professionalBody = Some(ProfessionalBodyNo),
                    hasChanged = true,
                    hasAccepted = true
                  )
                )
              )

              val complete = mock[Supervision]

              when(complete.isComplete) thenReturn true
              when(mockCacheMap.getEntry[Supervision]("supervision")) thenReturn Some(complete)

              val result = controller.post(true)(newRequest)
              status(result)           must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.SummaryController.get().url))
            }
          }
        }
      }
    }

    "on post with invalid data" in new Fixture {

      val newRequest = FakeRequest(POST, routes.ProfessionalBodyMemberController.post().url)
        .withFormUrlEncodedBody("" -> "")

      mockCacheFetch[Supervision](None)

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#isAMember]").html() must include(
        Messages("error.required.supervision.business.a.member")
      )
    }

  }

  it must {

    "remove professionalBodies data" when {
      "updated from ProfessionalBodyMemberYes to No" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ProfessionalBodyMemberController.post().url)
          .withFormUrlEncodedBody(
            "isAMember" -> "false"
          )

        mockCacheFetch[Supervision](
          Some(
            Supervision(
              professionalBodyMember = Some(ProfessionalBodyMemberYes),
              professionalBodies = Some(
                ProfessionalBodies(
                  Set(AccountantsEnglandandWales, Other("Another professional body"))
                )
              )
            )
          )
        )

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)

        verify(controller.dataCacheConnector).save[Supervision](
          any(),
          any(),
          eqTo(
            Supervision(
              professionalBodyMember = Some(ProfessionalBodyMemberNo),
              hasChanged = true
            )
          )
        )(any())

      }
      "ProfessionalBodyMemberNo and professionalBodies is defined" in new Fixture {

        val newRequest = FakeRequest(POST, routes.ProfessionalBodyMemberController.post().url)
          .withFormUrlEncodedBody(
            "isAMember" -> "false"
          )

        mockCacheFetch[Supervision](
          Some(
            Supervision(
              professionalBodies = Some(
                ProfessionalBodies(
                  Set(AccountantsEnglandandWales, Other("Another professional body"))
                )
              )
            )
          )
        )

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)

        verify(controller.dataCacheConnector).save[Supervision](
          any(),
          any(),
          eqTo(
            Supervision(
              professionalBodyMember = Some(ProfessionalBodyMemberNo),
              hasChanged = true
            )
          )
        )(any())

      }
    }

  }

}
