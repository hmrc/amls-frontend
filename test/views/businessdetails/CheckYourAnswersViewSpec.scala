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

package views.businessdetails

import models.businessdetails._
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.test.Injecting
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.AmlsSummaryViewSpec
import utils.businessdetails.CheckYourAnswersHelper
import views.Fixture
import views.html.businessdetails.CheckYourAnswersView

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class CheckYourAnswersViewSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks with Injecting {

  lazy val summary   = inject[CheckYourAnswersView]
  lazy val cyaHelper = inject[CheckYourAnswersHelper]

  "summary view" must {
    "have correct title" in new Fixture {

      def view = summary(SummaryList(Nil))

      doc.title must startWith(messages("title.cya") + " - " + messages("summary.businessdetails"))
    }

    "have correct headings" in new Fixture {
      def view = summary(SummaryList(Nil))

      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.businessdetails"))
    }

    "does not show registered for mlr question when approved" in new Fixture {
      def view = summary(SummaryList(Nil))

      html must not include messages("businessdetails.registeredformlr.title")
    }

    "include the provided data" in new Fixture {

      val list = cyaHelper.createSummaryList(
        BusinessDetails(
          Some(PreviouslyRegisteredYes(Some("1234"))),
          Some(ActivityStartDate(LocalDate.of(2016, 1, 2))),
          Some(VATRegisteredYes("2345")),
          Some(CorporationTaxRegisteredYes("3456")),
          Some(ContactingYou(Some("01234567890"), Some("test@test.com"))),
          Some(RegisteredOfficeIsUK(true)),
          Some(RegisteredOfficeUK("line1", Some("line2"), Some("line3"), Some("line4"), "AB12CD")),
          Some(true),
          Some(CorrespondenceAddressIsUk(true)),
          Some(
            CorrespondenceAddress(
              Some(
                CorrespondenceAddressUk(
                  "your name",
                  "business name",
                  "line1",
                  Some("line2"),
                  Some("line3"),
                  Some("line4"),
                  "AB12CD"
                )
              ),
              None
            )
          ),
          hasChanged = false
        ),
        showRegisteredForMLR = true
      )

      def view = summary(list)

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
