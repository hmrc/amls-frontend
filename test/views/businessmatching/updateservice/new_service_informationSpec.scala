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

package views.businessmatching.updateservice

import models.businessmatching.AccountancyServices
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import utils.GenericTestHelper
import views.Fixture
import views.html.businessmatching.updateservice.new_service_information

class new_service_informationSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {

    implicit override val request = addToken(FakeRequest())

    val nextPageUrl = controllers.asp.routes.WhatYouNeedController.get().url

    def view = new_service_information(AccountancyServices, nextPageUrl)

  }

  "The view template" must {
    "display the correct headings and titles" in new ViewFixture {
      validateTitle("businessmatching.updateservice.newserviceinformation.title")
      doc.select("h1").text mustBe messages("businessmatching.updateservice.newserviceinformation.heading")
    }

    "displays the correct url on the button link" in new ViewFixture {
      doc.select("input[name=\"redirectUrl\"]").attr("value") mustBe nextPageUrl
    }
  }

}
