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

package views.tcsp

import generators.AmlsReferenceNumberGenerator
import models.tcsp.ProvidedServices._
import models.tcsp.TcspTypes._
import models.tcsp._
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import utils.tcsp.CheckYourAnswersHelper
import views.Fixture
import views.html.tcsp.CheckYourAnswersView

import scala.jdk.CollectionConverters._

class CheckYourAnswersViewSpec
    extends AmlsSummaryViewSpec
    with TableDrivenPropertyChecks
    with AmlsReferenceNumberGenerator {

  lazy val summary   = inject[CheckYourAnswersView]
  lazy val cyaHelper = inject[CheckYourAnswersHelper]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView(FakeRequest())
  }

  "CheckYourAnswersView" must {
    "have correct title, heading and subheading" in new ViewFixture {

      def view = summary(cyaHelper.getSummaryList(Tcsp()))

      val title = messages("title.cya") + " - " + messages("summary.tcsp") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      doc.title       must be(title)
      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.tcsp"))
    }

    "include the provided data" in new ViewFixture {

      val list = cyaHelper.getSummaryList(
        Tcsp(
          Some(
            TcspTypes(
              Set(
                NomineeShareholdersProvider,
                TrusteeProvider,
                RegisteredOfficeEtc,
                CompanyDirectorEtc,
                CompanyFormationAgent
              )
            )
          ),
          Some(OnlyOffTheShelfCompsSoldYes),
          Some(ComplexCorpStructureCreationNo),
          Some(
            ProvidedServices(
              Set(
                PhonecallHandling,
                EmailHandling,
                EmailServer,
                SelfCollectMailboxes,
                MailForwarding,
                Receptionist,
                ConferenceRooms,
                Other("sfasfasef")
              )
            )
          ),
          Some(true),
          Some(ServicesOfAnotherTCSPYes(Some(amlsRegistrationNumber)))
        )
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
