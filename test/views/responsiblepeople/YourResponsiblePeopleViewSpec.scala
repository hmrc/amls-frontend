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

package views.responsiblepeople

import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.YourResponsiblePeopleView

class YourResponsiblePeopleViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val peopleView                                            = inject[YourResponsiblePeopleView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  val completePersonName: Option[PersonName]   = Some(PersonName("Katie", None, "Test"))
  val incompletePersonName: Option[PersonName] = Some(PersonName("John", None, "Test"))
  val completeRp1                              = ResponsiblePerson(completePersonName)
  val completeRp2                              = ResponsiblePerson()
  val incompleteRp1                            = ResponsiblePerson(incompletePersonName)
  val incompleteRp2                            = ResponsiblePerson()

  val completeRpSeq   = Seq((completeRp1, 0), (completeRp2, 1))
  val incompleteRpSeq = Seq((incompleteRp1, 2), (incompleteRp2, 3))

  "YourResponsiblePeopleView" must {
    "have correct title, headings, form fields and bullet list of types of RP's to register displayed" in new ViewFixture {

      def view = peopleView(Seq((ResponsiblePerson(), 0)), Seq((ResponsiblePerson(), 0)))

      doc.title       must be(
        messages("responsiblepeople.whomustregister.title") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must be(messages("responsiblepeople.whomustregister.title"))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("id", "addResponsiblePerson") must not be empty

      html must include(messages("responsiblepeople.whomustregister.line_1"))
      html must include(messages("responsiblepeople.whomustregister.line_2"))
      html must include(messages("responsiblepeople.whomustregister.line_3"))
      html must include(messages("responsiblepeople.whomustregister.line_4"))
      html must include(messages("responsiblepeople.whomustregister.line_5"))
    }

    "have list with Incomplete and Complete headers displayed when there are both types of lists" in new ViewFixture {
      def view = peopleView(completeRpSeq, incompleteRpSeq)

      html must include(messages("responsiblepeople.check_your_answers.incomplete"))
      html must include(messages("responsiblepeople.check_your_answers.complete"))
    }

    "have list with Incomplete header displayed when there are only incomplete RP's" in new ViewFixture {
      def view = peopleView(Seq.empty[(ResponsiblePerson, Int)], incompleteRpSeq)

      html must include(messages("responsiblepeople.check_your_answers.incomplete"))
      html must not include messages("responsiblepeople.check_your_answers.complete")
    }

    "have list without Complete/Incomplete headers displayed when there are only complete RP's" in new ViewFixture {
      def view = peopleView(completeRpSeq, Seq.empty[(ResponsiblePerson, Int)])

      html must not include messages("responsiblepeople.check_your_answers.incomplete")
      html must not include messages("responsiblepeople.check_your_answers.complete")
    }

    "have an add a responsible person link with the correct text and going to the what you need page" in new ViewFixture {
      def view = peopleView(completeRpSeq, incompleteRpSeq)

      doc.getElementById("addResponsiblePerson").text         must be(messages("responsiblepeople.check_your_answers.add"))
      doc.getElementById("addResponsiblePerson").attr("href") must be(
        controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(false).url
      )
    }

    "have an incomplete/complete sections with people names displayed and edit/remove links" in new ViewFixture {
      def view = peopleView(completeRpSeq, incompleteRpSeq)

      doc.getElementById("complete-header").text         must include(messages("responsiblepeople.check_your_answers.complete"))
      doc.getElementById("detail-edit-0").attr("href")   must be(
        controllers.responsiblepeople.routes.DetailedAnswersController.get(1).url
      )
      doc.getElementById("detail-remove-0").attr("href") must be(
        controllers.responsiblepeople.routes.RemoveResponsiblePersonController.get(1).url
      )

      doc.getElementById("incomplete-header").text       must include(
        messages("responsiblepeople.check_your_answers.incomplete")
      )
      doc.getElementById("incomplete-detail-2").text     must include("John Test")
      doc.getElementById("detail-edit-2").attr("href")   must be(
        controllers.responsiblepeople.routes.PersonNameController.get(3, false, None).url
      )
      doc.getElementById("detail-remove-2").attr("href") must be(
        controllers.responsiblepeople.routes.RemoveResponsiblePersonController.get(3).url
      )
    }
  }
}
