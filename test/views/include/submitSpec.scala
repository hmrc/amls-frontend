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

package views.include

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness.ActivityStartDate
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AmlsSpec
import views.Fixture
import views.html.include.forms2._

class submitSpec extends AmlsSpec with MustMatchers {
  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }
  "sumbit" must {
    "show the submit button" in {

      val t = submit()

      val doc = Jsoup.parse(contentAsString(t))

      doc.select("""button[type=submit][name=submit]""").text mustBe Messages("button.saveandcontinue")
    }

    "show the return to application progress link when required" in {

      val t = submit()

      val doc = Jsoup.parse(contentAsString(t))

      doc.getElementsMatchingOwnText(Messages("link.return.registration.progress")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("link.return.registration.progress")).attr("href") must be("/anti-money-laundering/registration-progress")
    }

    "show the return to renewal progress link when required" in {

      val t = submit(returnLocation = Some("renewal"))

      val doc = Jsoup.parse(contentAsString(t))

      doc.getElementsMatchingOwnText(Messages("link.return.renewal.progress")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("link.return.renewal.progress")).attr("href") must be("/anti-money-laundering/renewal/progress")
    }

    "not show the return to link when not needed" in {

      val t = submit(returnLink = false)

      val doc = Jsoup.parse(contentAsString(t))

      doc.select(".return-link a").isEmpty must be (true)
    }


  }
}