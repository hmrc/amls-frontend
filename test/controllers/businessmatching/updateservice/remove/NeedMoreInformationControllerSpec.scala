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

package controllers.businessmatching.updateservice.remove

import models.DateOfChange
import models.businessmatching.HighValueDealing
import models.flowmanagement.RemoveBusinessTypeFlowModel
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.Helpers.{OK, contentAsString, status, _}
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

class NeedMoreInformationControllerSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>

    val request = addToken(authRequest)

    val controller = new NeedMoreInformationController(
      authConnector = self.authConnector,
      dataCacheConnector = mockCacheConnector,
      router = createRouter[RemoveBusinessTypeFlowModel]
    )
  }


  "NeedToUpdateController" when {

    "get is called" must {

      "return OK with need_to_update view" in new Fixture {
        mockCacheFetch(Some(RemoveBusinessTypeFlowModel(Some(Set(HighValueDealing)))), Some(RemoveBusinessTypeFlowModel.key))

        val result = controller.get()(request)
        status(result) must be(OK)
        Jsoup.parse(contentAsString(result)).title() must include(Messages("businessmatching.updateservice.updateotherinformation.title"))
      }
    }

    "post is called" must {
      "redirect to next page" in new Fixture {
        val today = LocalDate.now
        mockCacheFetch(Some(RemoveBusinessTypeFlowModel()), Some(RemoveBusinessTypeFlowModel.key))

        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
      }
    }
  }

}