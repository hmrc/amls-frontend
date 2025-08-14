/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import utils.AmlsSpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.SignedOutView

class SignedOutControllerSpec extends AmlsSpec {

  "SignedOutController.get" must {
    "return 200 OK and render the view" in {
      val view       = app.injector.instanceOf[SignedOutView]
      val controller = new SignedOutController(ds = commonDependencies, cc = mockMcc, view = view)

      val req  = addToken(FakeRequest(GET, "/we-signed-you-out"))
      val msgs = messagesApi.preferred(req)

      val result = controller.get()(req)

      status(result) mustBe OK
      contentAsString(result) mustBe view()(msgs, req).toString
      contentType(result) mustBe Some("text/html")
      charset(result) mustBe Some("utf-8")
    }
  }
}
