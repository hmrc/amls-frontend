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

package views.responsiblepeople

import forms.EmptyForm
import models.bankdetails.BankDetails
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.openqa.selenium.WebElement
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.AmlsSpec
import views.Fixture


class your_responsible_peopleSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  val completePersonName: Option[PersonName] = Some(PersonName("Katie", None, "Test"))
  val incompletePersonName: Option[PersonName] = Some(PersonName("John", None, "Test"))
  val completeRp1 = ResponsiblePerson(completePersonName)
  val completeRp2 = ResponsiblePerson()
  val incompleteRp1 = ResponsiblePerson(incompletePersonName)
  val incompleteRp2 = ResponsiblePerson()

  val completeRpSeq = Seq((completeRp1,0),(completeRp2,1))
  val incompleteRpSeq = Seq((incompleteRp1,2),(incompleteRp2,3))

  "What you need View" must {
    "have correct title, headings, form fields and bullet list of types of RP's to register displayed" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.your_responsible_people(Seq((ResponsiblePerson(),0)), Seq((ResponsiblePerson(),0)))

      doc.title must be(Messages("responsiblepeople.whomustregister.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.whomustregister.title"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("id", "addResponsiblePerson") must not be empty

      html must include(Messages("responsiblepeople.whomustregister.line_1"))
      html must include(Messages("responsiblepeople.whomustregister.line_2"))
      html must include(Messages("responsiblepeople.whomustregister.line_3"))
      html must include(Messages("responsiblepeople.whomustregister.line_4"))
      html must include(Messages("responsiblepeople.whomustregister.line_5"))

    }

    "have list with Incomplete and Complete headers displayed when there are both types of lists" in new ViewFixture {
      def  view = views.html.responsiblepeople.your_responsible_people(completeRpSeq, incompleteRpSeq)

      html must include(Messages("responsiblepeople.check_your_answers.incomplete"))
      html must include(Messages("responsiblepeople.check_your_answers.complete"))

    }

    "have list with Incomplete header displayed when there are only incomplete RP's" in new ViewFixture {
      def  view = views.html.responsiblepeople.your_responsible_people(Seq.empty[(ResponsiblePerson, Int)], incompleteRpSeq)

      html must include(Messages("responsiblepeople.check_your_answers.incomplete"))
      html must not include Messages("responsiblepeople.check_your_answers.complete")

    }

    "have list without Complete/Incomplete headers displayed when there are only complete RP's" in new ViewFixture {
      def  view = views.html.responsiblepeople.your_responsible_people(completeRpSeq, Seq.empty[(ResponsiblePerson, Int)])

      html must not include Messages("responsiblepeople.check_your_answers.incomplete")
      html must not include Messages("responsiblepeople.check_your_answers.complete")
    }

    "have an add a responsible person link with the correct text and going to the what you need page" in new ViewFixture {
      def  view = views.html.responsiblepeople.your_responsible_people(completeRpSeq, incompleteRpSeq)

      doc.getElementById("addResponsiblePerson").text must be(Messages("responsiblepeople.check_your_answers.add"))
      doc.getElementById("addResponsiblePerson").attr("href") must be(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(false).url)
    }

    "have an incomplete/complete sections with people names displayed and edit/remove links" in new ViewFixture {
      def view = views.html.responsiblepeople.your_responsible_people(completeRpSeq, incompleteRpSeq)

      doc.getElementById("complete-header").text must include(Messages("responsiblepeople.check_your_answers.complete"))
      doc.getElementById("complete-detail-0").text must include("Katie Test")
      doc.getElementById("detail-edit-0").attr("href") must be(controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url)
      doc.getElementById("detail-remove-0").attr("href") must be(controllers.responsiblepeople.routes.RemoveResponsiblePersonController.get(1).url)

      doc.getElementById("incomplete-header").text must include(Messages("responsiblepeople.check_your_answers.incomplete"))
      doc.getElementById("incomplete-detail-2").text must include("John Test")
      doc.getElementById("detail-edit-2").attr("href") must be(controllers.responsiblepeople.routes.PersonNameController.get(3, false, None).url)
      doc.getElementById("detail-remove-2").attr("href") must be(controllers.responsiblepeople.routes.RemoveResponsiblePersonController.get(3).url)

    }
  }
}
