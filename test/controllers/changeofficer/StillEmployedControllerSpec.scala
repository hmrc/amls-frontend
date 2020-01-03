/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.changeofficer

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.responsiblepeople._
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class StillEmployedControllerSpec extends AmlsSpec {

  trait TestFixture extends AuthorisedFixture { self =>
    val request = addToken(self.authRequest)

    val cache = mock[DataCacheConnector]

    val controller = new StillEmployedController(SuccessfulAuthAction, commonDependencies, cache, mockMcc)

    val nominatedOfficer = ResponsiblePerson(
      personName = Some(PersonName("firstName", None, "lastName")),
      positions = Some(Positions(Set(NominatedOfficer),None))
    )

    val otherResponsiblePerson = ResponsiblePerson(
      personName = Some(PersonName("otherFirstName", None, "otherLastName")),
      positions = Some(Positions(Set(Director),None))
    )

    when(cache.fetch[Seq[ResponsiblePerson]](any(),any())( any(), any()))
      .thenReturn(Future.successful(Some(Seq(nominatedOfficer, otherResponsiblePerson))))
  }

  "The StillEmployedController" when {
    "get is called" must {

      "respond with OK and include the person name" in new TestFixture {

        val result = controller.get()(request)

        status(result) mustBe OK
        contentAsString(result) must include("firstName lastName")
      }

      "redirect to NewOfficerController" when {
        "no nominated officer is found" in new TestFixture {

          when(cache.fetch[Seq[ResponsiblePerson]](any(),any())(any(), any()))
            .thenReturn(Future.successful(Some(Seq(otherResponsiblePerson))))

          val result = controller.get()(request)

          status(result) mustBe SEE_OTHER
          redirectLocation(result) must be(Some(routes.NewOfficerController.get().url))
        }
      }
    }

    "post is called" must {
      "respond with BAD_REQUEST" in new TestFixture {
        val result = controller.post()(request)
        status(result) mustBe BAD_REQUEST
      }

      "respond with SEE_OTHER" when {
        "request is 'yes'" which {
          "redirects to RoleInBusinessController" in new TestFixture {
            val result = controller.post()(requestWithUrlEncodedBody("stillEmployed" -> "true"))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.changeofficer.routes.RoleInBusinessController.get().url)
          }
        }
        "request is 'no'" which {
          "redirects to RemoveResponsiblePerson" in new TestFixture {
            val result = controller.post()(requestWithUrlEncodedBody("stillEmployed" -> "false"))
            status(result) mustBe SEE_OTHER
            redirectLocation(result) mustBe Some(controllers.changeofficer.routes.RemoveResponsiblePersonController.get().url)
          }
        }
      }
    }
  }
}