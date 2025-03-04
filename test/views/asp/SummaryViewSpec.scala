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

package views.asp

import models.asp._
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import views.Fixture
import views.html.asp.SummaryView

import scala.jdk.CollectionConverters._

class SummaryViewSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks {

  trait ViewFixture extends Fixture {
    lazy val summary                                               = app.injector.instanceOf[SummaryView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView(FakeRequest())
  }

  "summary view" must {
    "have correct title" in new ViewFixture {

      def view = summary(Asp())

      doc.title must startWith(messages("title.cya") + " - " + messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {
      def view = summary(Asp())

      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.asp"))
    }

    "include the provided data" in new ViewFixture {

      val list = Seq(
        (messages("asp.services.title"), Service.all.map(_.getMessage).sorted.mkString(" ")),
        (messages("asp.other.business.tax.matters.title"), messages("lbl.yes"))
      )

      def view = {
        val testdata = Asp(
          Some(ServicesOfBusiness(Service.all.toSet)),
          Some(OtherBusinessTaxMattersYes)
        )

        summary(testdata)
      }

      doc
        .getElementsByClass("govuk-summary-list__key")
        .asScala
        .zip(
          doc.getElementsByClass("govuk-summary-list__value").asScala
        )
        .foreach { case (key, value) =>
          val maybeRow = list.find(_._1 == key.text())

          maybeRow.value._1 mustBe key.text()
          maybeRow.value._2 mustBe value.text()
        }
    }
  }
}
