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

package views.supervision

import models.supervision.ProfessionalBodies._
import models.supervision._
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import utils.supervision.CheckYourAnswersHelper
import views.Fixture
import views.html.supervision.CheckYourAnswersView

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class CheckYourAnswersViewSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    lazy val checkYourAnswersView                                  = inject[CheckYourAnswersView]
    lazy val cyaHelper                                             = inject[CheckYourAnswersHelper]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView(FakeRequest())

    val start  = Some(SupervisionStart(LocalDate.of(1990, 2, 24))) // scalastyle:off magic.number
    val end    = Some(SupervisionEnd(LocalDate.of(1998, 2, 24))) // scalastyle:off magic.number
    val reason = Some(SupervisionEndReasons("Ending reason"))
  }

  "CheckYourAnswersView" must {
    "have correct title" in new ViewFixture {
      def view = checkYourAnswersView(cyaHelper.getSummaryList(Supervision()))

      doc.title must startWith(messages("title.cya") + " - " + messages("summary.supervision"))
    }

    "have correct headings" in new ViewFixture {
      def view = checkYourAnswersView(cyaHelper.getSummaryList(Supervision()))

      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.supervision"))
    }

    "include the provided data if there is another body provided" in new ViewFixture {

      val list = cyaHelper.getSummaryList(
        Supervision(
          Some(AnotherBodyYes("Company A", start, end, reason)),
          Some(ProfessionalBodyMemberYes),
          Some(
            ProfessionalBodies(
              Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("anotherProfessionalBody"))
            )
          ),
          Some(ProfessionalBodyYes("details")),
          hasAccepted = true
        )
      )

      def view = checkYourAnswersView(list)

      doc
        .getElementsByClass("govuk-summary-list__key")
        .asScala
        .zip(
          doc.getElementsByClass("govuk-summary-list__value").asScala
        )
        .foreach { case (key, value) =>
          val maybeRow = list.rows.find(_.key.content.asHtml.body == key.text()).value

          maybeRow.key.content.asHtml.body must include(key.text())

          val valueText = maybeRow.value.content.asHtml.body match {
            case str if str.startsWith("<") => Jsoup.parse(str).text()
            case str                        => str
          }

          valueText must include(value.text())
        }
    }

    "include the provided data if there is no another body provided" in new ViewFixture {

      val list = cyaHelper.getSummaryList(
        Supervision(
          Some(AnotherBodyNo),
          Some(ProfessionalBodyMemberYes),
          Some(
            ProfessionalBodies(
              Set(AccountingTechnicians, CharteredCertifiedAccountants, Other("anotherProfessionalBody"))
            )
          ),
          Some(ProfessionalBodyYes("details")),
          hasAccepted = true
        )
      )

      def view = checkYourAnswersView(list)

      doc
        .getElementsByClass("govuk-summary-list__key")
        .asScala
        .zip(
          doc.getElementsByClass("govuk-summary-list__value").asScala
        )
        .foreach { case (key, value) =>
          val maybeRow = list.rows.find(_.key.content.asHtml.body == key.text()).value

          maybeRow.key.content.asHtml.body must include(key.text())

          val valueText = maybeRow.value.content.asHtml.body match {
            case str if str.startsWith("<") => Jsoup.parse(str).text()
            case str                        => str
          }

          valueText must include(value.text())
        }
    }
  }
}
