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

package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessType
import org.jsoup.nodes.{Document, Element}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class position_within_businessSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
    val name = "firstName lastName"

    def validateOtherSelection = {
      val (otherCheckbox, otherLabel) = checkboxAndLabel("positions-other")(doc)
      otherCheckbox mustBe defined
      otherLabel mustBe defined
      otherLabel map (_.text must include(Messages("responsiblepeople.position_within_business.lbl.09")))
    }

  }

  def checkboxAndLabel(id: String)(implicit doc: Document): (Option[Element], Option[Element]) = {
    val cb = Option(doc.getElementById(id))
    val lbl = cb.fold[Option[Element]](None)(c => Option(c.parent))
    (cb, lbl)
  }

  "position_within_business view" must {
    "have correct title, headings" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.position_within_business(form2, true, 1, BusinessType.SoleProprietor, name, true, None)

      doc.title must be(Messages("responsiblepeople.position_within_business.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.position_within_business.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "positions[]") must not be empty
    }

    "have the correct fields" when {

      def assertLabelIncluded(i: Int = 1)(implicit positions: List[Int], formText: String, html: Document): Unit = {

        val (checkbox, label) = checkboxAndLabel(s"positions-0$i")

        if (i <= 9) {

          if (positions contains i) {
            checkbox mustBe defined
            label mustBe defined
            label map {_.text() must include(Messages(s"responsiblepeople.position_within_business.lbl.0$i"))}

            assertLabelIncluded(i + 1)
          } else {
            checkbox must not be defined
            label must not be defined

            assertLabelIncluded(i + 1)
          }
        }
      }

      val testCases = List(
        BusinessType.SoleProprietor -> List(4, 6),
        BusinessType.Partnership -> List(4, 5),
        BusinessType.LimitedCompany -> List(1, 2, 4),
        BusinessType.UnincorporatedBody -> List(1, 2, 4),
        BusinessType.LPrLLP -> List(4, 5, 7)
      )

      "nominated officer has not been selected previously" when {
        testCases foreach {
          case (businessType, positionsToDisplay) => {
            s"$businessType" in new ViewFixture {

              def view = views.html.responsiblepeople.position_within_business(EmptyForm, true, 1, businessType, name, true, None)



              assertLabelIncluded()(positionsToDisplay, form.text, doc)
            }
          }
        }
      }

      "nominated officer has been selected previously" when {
        testCases foreach {
          case (businessType, positionsToDisplay) => {
            s"$businessType" in new ViewFixture {

              def view = views.html.responsiblepeople.position_within_business(EmptyForm, true, 1, businessType, name, false, None)

              validateOtherSelection
              assertLabelIncluded()(positionsToDisplay.filterNot(_.equals(4)), form.text, doc)
            }
          }
        }
      }
    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "positions") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.position_within_business(form2, true, 1, BusinessType.SoleProprietor, name, true, None)

      validateOtherSelection
      errorSummary.html() must include("not a message Key")
    }
  }
}
