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

package controllers.supervision

import models.supervision._
import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class ProfessionalBodyMemberControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {self =>
    val request = addToken(authRequest)

    val controller = new ProfessionalBodyMemberController (
      dataCacheConnector = mockCacheConnector,
      authConnector = self.authConnector
    )

    mockCacheSave[Supervision]

  }

  "ProfessionalBodyMemberController" must {

    "load the page Is your business a member of a professional body?" in new Fixture  {

      mockCacheFetch[Supervision](None)

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("supervision.memberofprofessionalbody.title"))

    }

    "load the page Is your business a member of a professional body? with pre-populate data" in new Fixture  {

      mockCacheFetch[Supervision](Some(Supervision(
        professionalBodyMember = Some(ProfessionalBodyMemberYes)
      )))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=true]").hasAttr("checked") must be(true)

    }

    "on post with valid data" must {
      "redirect to WhichProfessionalBodyController" when {
        "isMember is true" when {
          "edit is false" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "isAMember" -> "true"
            )

            mockCacheFetch[Supervision](None)

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.WhichProfessionalBodyController.get().url))
          }
          "edit is true" when {
            "professionalBodies is not defined" in new Fixture {

              val newRequest = request.withFormUrlEncodedBody(
                "isAMember" -> "true"
              )

              mockCacheFetch[Supervision](None)

              val result = controller.post(true)(newRequest)
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.WhichProfessionalBodyController.get(true).url))
            }
          }
        }
      }
      "redirect to PenalisedByProfessionalController" when {
        "isMember is false" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "isAMember" -> "false"
          )

          mockCacheFetch[Supervision](None)

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.PenalisedByProfessionalController.get().url))
        }
      }
      "redirect to SummaryController" when {
        "edit is true" when {
          "isMember is true" when {
            "ProfessionalBodyMemberYes is already defined" in new Fixture {

              val newRequest = request.withFormUrlEncodedBody(
                "isAMember" -> "true"
              )

              mockCacheFetch[Supervision](Some(Supervision(
                professionalBodyMember = Some(ProfessionalBodyMemberYes)
              )))

              val result = controller.post(true)(newRequest)
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(routes.SummaryController.get().url))
            }
          }
        }
      }
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()

      mockCacheFetch[Supervision](None)

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#isAMember]").html() must include(Messages("error.required.supervision.business.a.member"))
    }

  }

  it must {

    "remove professionalBodies data" when {
      "updated from ProfessionalBodyMemberYes to No" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isAMember" -> "false"
        )

        mockCacheFetch[Supervision](Some(Supervision(
          professionalBodyMember = Some(ProfessionalBodyMemberYes),
          professionalBodies = Some(ProfessionalBodies(
            Set(AccountantsEnglandandWales, Other("Another professional body"))
          ))
        )))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)

        verify(controller.dataCacheConnector).save[Supervision](any(),eqTo(Supervision(
          professionalBodyMember = Some(ProfessionalBodyMemberNo),
          hasChanged = true
        )))(any(),any(),any())

      }
      "ProfessionalBodyMemberNo and professionalBodies is defined" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isAMember" -> "false"
        )

        mockCacheFetch[Supervision](Some(Supervision(
          professionalBodies = Some(ProfessionalBodies(
            Set(AccountantsEnglandandWales, Other("Another professional body"))
          ))
        )))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)

        verify(controller.dataCacheConnector).save[Supervision](any(),eqTo(Supervision(
          professionalBodyMember = Some(ProfessionalBodyMemberNo),
          hasChanged = true
        )))(any(),any(),any())

      }
    }

  }

}
