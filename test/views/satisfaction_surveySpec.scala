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

package views

import java.net.URI

import forms.{EmptyForm, InvalidForm}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, contentAsString, status}
import utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError

class satisfaction_surveySpec extends AmlsSpec with MustMatchers with MockitoSugar {

  trait SatisfactionSurveyFixture {
    implicit val request : Request[_] = addToken(FakeRequest())
    val view = views.html.satisfaction_survey(EmptyForm)
    lazy val html = view.body
    lazy val doc = Jsoup.parse(html)
    lazy val form = doc.getElementsByTag("form").first()
  }

  "The satisfaction survey view" should {
    "contain a form" which {
      "posts it's data" in new SatisfactionSurveyFixture {
        form.attr("method") must be("POST")
      }

      "targets the correct route" in new SatisfactionSurveyFixture {
        val x = new URI(form.attr("action")).getPath
        x mustBe (controllers.routes.SatisfactionSurveyController.post().url)
      }

    "contain 5 checkboxes with the correct titles" in new SatisfactionSurveyFixture {
      form.select("input[type=radio]").size() must be (5)
      form.select("[for=satisfaction-01]").text() mustBe Messages("survey.satisfaction.lbl.01")
      form.select("[for=satisfaction-02]").text() mustBe Messages("survey.satisfaction.lbl.02")
      form.select("[for=satisfaction-03]").text() mustBe Messages("survey.satisfaction.lbl.03")
      form.select("[for=satisfaction-04]").text() mustBe Messages("survey.satisfaction.lbl.04")
      form.select("[for=satisfaction-05]").text() mustBe Messages("survey.satisfaction.lbl.05")
    }

    "Has a comment box" in new SatisfactionSurveyFixture {
        Option(form.getElementById("details")) mustBe defined
    }

    "Displays an error in the top and above the field" in new SatisfactionSurveyFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "satisfaction") -> Seq(ValidationError("Error found here"))
        ))

      override val view = views.html.satisfaction_survey(form2)
      doc.select(".validation-summary-message").text().contains("Error found here") mustBe true
      doc.select(".error-notification").text().contains("Error found here") mustBe true
    }

    "Includes the correct heading and section" in new SatisfactionSurveyFixture {
        doc.body().select("h1").text() mustBe Messages("survey.satisfaction.title")
        doc.body().select(".heading-secondary").text().contains(Messages("survey.satisfaction.heading")) mustBe true
    }

    "Includes the correct page title" in new SatisfactionSurveyFixture {
      doc.title must be(Messages("survey.satisfaction.title") +
        " - " + Messages("survey.satisfaction.heading") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }


    "Includes the correct sub and hint text" in new SatisfactionSurveyFixture {
      doc.select("#satisfaction > legend > span").text() mustBe Messages("survey.satisfaction.text.1")
      doc.select("#details > legend > span").text() mustBe Messages("survey.satisfaction.text.2")
      doc.select("#details-hint").text() mustBe Messages("survey.satisfaction.hint")
    }

    "Includes a submit button with the correct text" in new SatisfactionSurveyFixture {
      doc.getElementById("satisfaction-submit").attr("value") mustBe Messages("button.send.feedback")
    }
    }
  }
}
