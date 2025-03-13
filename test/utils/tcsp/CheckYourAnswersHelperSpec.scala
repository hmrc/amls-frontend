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

package utils.tcsp

import controllers.actions.AuthActionSpec.amlsRegistrationNumber
import models.tcsp.ProvidedServices._
import models.tcsp.TcspTypes._
import models.tcsp._
import org.scalatest.Assertion
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.AmlsSpec

import scala.collection.immutable.ListSet

class CheckYourAnswersHelperSpec extends AmlsSpec {

  lazy val cyaHelper: CheckYourAnswersHelper = app.injector.instanceOf[CheckYourAnswersHelper]

  val otherService = "Carrier pigeon"

  val tcspTypes: TcspTypes = TcspTypes(
    Set(NomineeShareholdersProvider, TrusteeProvider, RegisteredOfficeEtc, CompanyDirectorEtc, CompanyFormationAgent)
  )

  val providedServices: ProvidedServices = ProvidedServices(
    ListSet(
      PhonecallHandling,
      EmailHandling,
      EmailServer,
      SelfCollectMailboxes,
      MailForwarding,
      Receptionist,
      ConferenceRooms,
      Other(otherService)
    )
  )

  val model: Tcsp = Tcsp(
    Some(tcspTypes),
    Some(OnlyOffTheShelfCompsSoldYes),
    Some(ComplexCorpStructureCreationYes),
    Some(providedServices),
    Some(true),
    Some(ServicesOfAnotherTCSPYes(Some(amlsRegistrationNumber)))
  )

  trait RowFixture {

    val summaryListRows: Seq[SummaryListRow]

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

    def checkChangeLink(slr: SummaryListRow, href: String, id: String): Assertion = {
      val changeLink = slr.actions.flatMap(_.items.headOption).getOrElse(fail("No edit link present"))

      changeLink.content.toString must include(messages("button.edit"))
      changeLink.href mustBe href
      changeLink.attributes("id") mustBe id
    }

    def toBulletList[A](coll: Seq[A]): String =
      "<ul class=\"govuk-list govuk-list--bullet\">" +
        coll.map { x =>
          s"<li>$x</li>"
        }.mkString +
        "</ul>"

    def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
      messages("lbl.yes")
    } else {
      messages("lbl.no")
    }
  }

  ".createSummaryList" when {

    "TCSP Types is present" must {

      "render the correct content for multiple types" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        val answer: String = toBulletList(
          Seq(
            messages(s"tcsp.service.provider.lbl.04"),
            messages(s"tcsp.service.provider.lbl.01"),
            messages(s"tcsp.service.provider.lbl.03"),
            messages(s"tcsp.service.provider.lbl.02"),
            messages(s"tcsp.service.provider.lbl.05")
          )
        )

        assertRowMatches(
          0,
          "tcsp.kind.of.service.provider.title",
          answer,
          controllers.tcsp.routes.TcspTypesController.get(true).url,
          "tcspkindserviceprovider-edit"
        )
      }

      "render the correct content for a single types" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(tcspTypes = Some(TcspTypes(Set(NomineeShareholdersProvider))))
          )
          .rows

        assertRowMatches(
          0,
          "tcsp.kind.of.service.provider.title",
          messages(s"tcsp.service.provider.lbl.${NomineeShareholdersProvider.value}"),
          controllers.tcsp.routes.TcspTypesController.get(true).url,
          "tcspkindserviceprovider-edit"
        )
      }
    }

    "Only Off The Shelf Comps Sold is present" must {

      "render the correct content for yes" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          1,
          "tcsp.off-the-shelf.companies.lbl",
          booleanToLabel(true),
          controllers.tcsp.routes.OnlyOffTheShelfCompsSoldController.get(true).url,
          "onlyOffTheShelfCompsSold-edit"
        )
      }

      "render the correct content for no" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(onlyOffTheShelfCompsSold = Some(OnlyOffTheShelfCompsSoldNo))
          )
          .rows

        assertRowMatches(
          1,
          "tcsp.off-the-shelf.companies.lbl",
          booleanToLabel(false),
          controllers.tcsp.routes.OnlyOffTheShelfCompsSoldController.get(true).url,
          "onlyOffTheShelfCompsSold-edit"
        )
      }
    }

    "Complex Corp Structure Creation is present" must {

      "render the correct content for yes" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          2,
          "tcsp.create.complex.corporate.structures.lbl",
          booleanToLabel(true),
          controllers.tcsp.routes.ComplexCorpStructureCreationController.get(true).url,
          "complexCorpStructureCreation-edit"
        )
      }

      "render the correct content for no" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(complexCorpStructureCreation = Some(ComplexCorpStructureCreationNo))
          )
          .rows

        assertRowMatches(
          2,
          "tcsp.create.complex.corporate.structures.lbl",
          booleanToLabel(false),
          controllers.tcsp.routes.ComplexCorpStructureCreationController.get(true).url,
          "complexCorpStructureCreation-edit"
        )
      }
    }

    "Provided Services Row is present" must {

      "render the correct content for multiple services" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          3,
          "tcsp.provided_services.title",
          toBulletList(providedServices.services.toSeq.map(_.getMessage)),
          controllers.tcsp.routes.ProvidedServicesController.get(true).url,
          "tcsptypes-edit"
        )
      }

      "render the correct content for a single service" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(providedServices = Some(ProvidedServices(Set(EmailServer))))
          )
          .rows

        assertRowMatches(
          3,
          "tcsp.provided_services.title",
          EmailServer.getMessage,
          controllers.tcsp.routes.ProvidedServicesController.get(true).url,
          "tcsptypes-edit"
        )
      }
    }

    "Does Services Of Another TCSP is present" must {

      "render the correct content for yes" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          4,
          "tcsp.servicesOfAnotherTcsp.title",
          booleanToLabel(true),
          controllers.tcsp.routes.ServicesOfAnotherTCSPController.get(true).url,
          "servicesofanothertcsp-edit"
        )
      }

      "render the correct content for no" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(doesServicesOfAnotherTCSP = Some(false))
          )
          .rows

        assertRowMatches(
          4,
          "tcsp.servicesOfAnotherTcsp.title",
          booleanToLabel(false),
          controllers.tcsp.routes.ServicesOfAnotherTCSPController.get(true).url,
          "servicesofanothertcsp-edit"
        )
      }
    }

    "Services Of Another TCSP is present" must {

      "render the correct content for yes" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper.getSummaryList(model).rows

        assertRowMatches(
          5,
          "tcsp.anothertcspsupervision.title",
          booleanToLabel(true),
          controllers.tcsp.routes.AnotherTCSPSupervisionController.get(true).url,
          "anothertcsp-edit"
        )

        assertRowMatches(
          6,
          "tcsp.anothertcspsupervision.cya.additional.header",
          amlsRegistrationNumber,
          controllers.tcsp.routes.AnotherTCSPSupervisionController.get(true).url,
          "mlrrefnumber-edit"
        )
      }

      "render the correct content for no" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = cyaHelper
          .getSummaryList(
            model.copy(servicesOfAnotherTCSP = Some(ServicesOfAnotherTCSPNo))
          )
          .rows

        assertRowMatches(
          5,
          "tcsp.anothertcspsupervision.title",
          booleanToLabel(false),
          controllers.tcsp.routes.AnotherTCSPSupervisionController.get(true).url,
          "anothertcsp-edit"
        )
      }
    }
  }
}
