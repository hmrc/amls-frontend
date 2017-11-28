/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.estateagentbusiness

import models.businessmatching.{EstateAgentBusinessService => EAB}
import models.estateagentbusiness._
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers.{eq => meq}
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class BusinessServicesControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    val controller = new BusinessServicesController(
      self.authConnector,
      mockCacheConnector,
      mockStatusService,
      mockServiceFlow
    )

    mockIsNewActivity(false, Some(EAB))

  }

  "BusinessServicesController" when {

    "get is called" must {

      "display Business services page" in new Fixture {

        mockCacheFetch[EstateAgentBusiness](None)

        val result = controller.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("estateagentbusiness.services.title"))
      }

      "load the page with data when the user revisits at a later time" in new Fixture {

        mockCacheFetch[EstateAgentBusiness](Some(EstateAgentBusiness(Some(Services(Set(Auction, Residential))), None, None, None)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document: Document = Jsoup.parse(contentAsString(result))
        document.select("input[value=03]").hasAttr("checked") must be(true)
        document.select("input[value=01]").hasAttr("checked") must be(true)
      }

    }

    "post is called" when {

      "valid data is submitted" must {

        "redirect to PenalisedUnderEstateAgentsActController" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "services[0]" -> "02",
            "services[1]" -> "08"
          )
          val eab = EstateAgentBusiness(Some(Services(Set(Residential))), Some(ThePropertyOmbudsman), None, None)

          val eabWithoutRedress = EstateAgentBusiness(Some(Services(Set(Commercial, Development), None)), None, None, None, true)

          mockApplicationStatus(SubmissionDecisionRejected)

          mockCacheFetch[EstateAgentBusiness](Some(eab))
          mockCacheSave[EstateAgentBusiness](eabWithoutRedress, Some(EstateAgentBusiness.key))

          val result = controller.post()(newRequest)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.PenalisedUnderEstateAgentsActController.get().url))
        }

        "redirect to ResidentialRedressSchemeController" when {

          "Residential option is submitted" when {

            "edit is true" in new Fixture {

              val newRequest = request.withFormUrlEncodedBody(
                "services[1]" -> "02",
                "services[0]" -> "01",
                "services[2]" -> "03"
              )
              val eab = EstateAgentBusiness(Some(Services(Set(Auction, Commercial, Residential))), None, None, None)

              mockApplicationStatus(SubmissionReadyForReview)

              mockCacheFetch[EstateAgentBusiness](Some(eab), Some(EstateAgentBusiness.key))
              mockCacheSave[EstateAgentBusiness](eab, Some(EstateAgentBusiness.key))

              val result = controller.post(true)(newRequest)
              status(result) must be(SEE_OTHER)

              redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ResidentialRedressSchemeController.get(true).url))
            }

            "edit is false" in new Fixture {

              val newRequest = request.withFormUrlEncodedBody(
                "services[0]" -> "01",
                "services[1]" -> "02",
                "services[2]" -> "03"
              )

              val eab = EstateAgentBusiness(Some(Services(Set(Auction, Commercial, Residential))), Some(ThePropertyOmbudsman), None, None)

              mockApplicationStatus(SubmissionReadyForReview)

              mockCacheFetch[EstateAgentBusiness](Some(eab), Some(EstateAgentBusiness.key))
              mockCacheSave[EstateAgentBusiness](eab, Some(EstateAgentBusiness.key))

              val result = controller.post()(newRequest)
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ResidentialRedressSchemeController.get().url))

            }

          }

        }

        "redirect to dateOfChange page" when {
          "EstateAgentBusiness is not newly added" when {
            "edit is true" when {
              "status is approved" in new Fixture {

                val newRequest = request.withFormUrlEncodedBody(
                  "services[0]" -> "01",
                  "services[1]" -> "02",
                  "services[2]" -> "07"
                )

                mockApplicationStatus(SubmissionDecisionApproved)

                mockCacheFetch[EstateAgentBusiness](Some(EstateAgentBusiness(
                  services = Some(Services(Set(Residential, Commercial, Auction)))
                )))
                mockCacheSave[EstateAgentBusiness]

                val result = controller.post()(newRequest)
                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ServicesDateOfChangeController.get().url))
              }

              "status is ready for renewal" in new Fixture {

                val newRequest = request.withFormUrlEncodedBody(
                  "services[0]" -> "01",
                  "services[1]" -> "02",
                  "services[2]" -> "07"
                )

                mockApplicationStatus(ReadyForRenewal(None))

                mockCacheFetch[EstateAgentBusiness](Some(EstateAgentBusiness(
                  services = Some(Services(Set(Residential, Commercial, Auction)))
                )))
                mockCacheSave[EstateAgentBusiness]

                val result = controller.post()(newRequest)
                status(result) must be(SEE_OTHER)
                redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.ServicesDateOfChangeController.get().url))
              }
            }
          }
        }

        "redirect to SummaryController" when {

          "edit is true" when {
            "status is pre-approved" in new Fixture {

              val newRequest = request.withFormUrlEncodedBody(
                "services[1]" -> "02",
                "services[2]" -> "07"
              )

              mockApplicationStatus(SubmissionReadyForReview)

              mockCacheFetch[EstateAgentBusiness](Some(EstateAgentBusiness(
                services = Some(Services(Set(Commercial, Auction)))
              )))
              mockCacheSave[EstateAgentBusiness]

              val result = controller.post(true)(newRequest)
              status(result) must be(SEE_OTHER)
              redirectLocation(result) must be(Some(controllers.estateagentbusiness.routes.SummaryController.get().url))

            }

          }

        }

      }

      "invalid data is submitted" must {

        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "services" -> "0299999"
          )

          mockCacheFetch[EstateAgentBusiness](None)
          mockCacheSave[EstateAgentBusiness]

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

        }

      }
    }
  }
}
