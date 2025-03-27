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

package utils.tradingpremises

import generators.tradingpremises.TradingPremisesGenerator
import models.businessmatching.BusinessActivities
import models.tradingpremises.BusinessStructure._
import models.tradingpremises._
import org.scalatest.Assertion
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{AmlsSpec, DateHelper}

import java.time.LocalDate

class CheckYourAnswersHelperSpec extends AmlsSpec with TradingPremisesGenerator {

  lazy val cyaHelper: CheckYourAnswersHelper = app.injector.instanceOf[CheckYourAnswersHelper]

  val agentPremisesRowIndex = 0
  val index                 = 1

  val ytp = YourTradingPremises(
    "foo",
    Address("123 Test Road", Some("Some Village"), Some("A Town"), Some("Big City"), "AA1 1BB", None),
    Some(true),
    Some(LocalDate.of(2010, 10, 10)),
    None
  )

  val tp: TradingPremises = TradingPremises(
    yourTradingPremises = Some(ytp),
    whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(BusinessActivities.all)),
    msbServices = Some(TradingPremisesMsbServices(TradingPremisesMsbService.all.toSet)),
    hasAccepted = true,
    hasChanged = true
  )

  val agentName             = "John Doe"
  val agentCompanyName      = "Agent Company Name"
  val agentCompanyRegNumber = "98765432"
  val agentPartnershipName  = "Agent Partner"
  val agentDob              = LocalDate.of(1995, 12, 12)

  val nonAgent: TradingPremises = tp.copy(
    registeringAgentPremises = Some(RegisteringAgentPremises(false)),
    businessStructure = None,
    agentName = None,
    agentCompanyDetails = None,
    agentPartnership = None
  )

  val agentPartnership: TradingPremises =
    tp.copy(
      registeringAgentPremises = Some(RegisteringAgentPremises(true)),
      businessStructure = Some(Partnership),
      agentName = None,
      agentCompanyDetails = None,
      agentPartnership = Some(AgentPartnership(agentPartnershipName))
    )

  val agentIncorporatedBody: TradingPremises = agentPartnership.copy(
    businessStructure = Some(IncorporatedBody),
    agentCompanyDetails = Some(AgentCompanyDetails(agentCompanyName, Some(agentCompanyRegNumber))),
    agentPartnership = None
  )

  val agentLimitedLiabilityPartnership: TradingPremises = agentIncorporatedBody.copy(
    businessStructure = Some(LimitedLiabilityPartnership)
  )

  val agentSoleProprietor: TradingPremises = agentIncorporatedBody.copy(
    businessStructure = Some(SoleProprietor),
    agentName = Some(AgentName(agentName, None, Some(agentDob))),
    agentCompanyDetails = None
  )

  val agentUnincorporatedBody: TradingPremises = agentSoleProprietor.copy(
    businessStructure = Some(UnincorporatedBody),
    agentName = None
  )

  private def getRows(premises: TradingPremises): Seq[SummaryListRow] =
    cyaHelper.createSummaryList(premises, index, true, false, false).rows

  val answersList = Seq(
    agentPartnership,
    agentLimitedLiabilityPartnership,
    agentSoleProprietor,
    agentUnincorporatedBody,
    agentIncorporatedBody
  )

  private def nameAndAddressToLines(name: String, address: Address) =
    "<ul class=\"govuk-list\">" +
      s"<li>$name</li>" +
      address.toLines.map { line =>
        s"""<li>$line<li>"""
      }.mkString + "</ul>"

  private def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
    messages("lbl.yes")
  } else {
    messages("lbl.no")
  }

  private def toBulletList[A](coll: Seq[A], messagePrefix: String)(implicit messages: Messages) =
    "<ul class=\"govuk-list govuk-list--bullet\">" +
      coll.map { x =>
        s"<li>${messages(s"$messagePrefix.$x")}</li>"
      }.mkString +
      "</ul>"
  trait RowFixture {

    val summaryListRows: Seq[SummaryListRow]

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

    def assertRowMatchesNoChangeLink(index: Int, title: String, value: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)
    }

    def checkChangeLink(slr: SummaryListRow, href: String, id: String): Assertion = {
      val changeLink = slr.actions.flatMap(_.items.headOption).getOrElse(fail("No edit link present"))

      changeLink.content.toString must include(messages("button.edit"))
      changeLink.href mustBe href
      changeLink.attributes("id") mustBe id
    }
  }

  ".createSummaryList" when {

    "Agent premises row is rendered" must {

      "display false for non-agents" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows(nonAgent)

        assertRowMatches(
          agentPremisesRowIndex,
          "tradingpremises.agent.premises.title",
          booleanToLabel(false),
          controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(index, true).url,
          "tradingpremisessummarywho-edit"
        )
      }

      answersList.foreach { tp =>
        val businessType = tp.businessStructure.getOrElse(fail("No Business Structure present")).toString

        s"display true for Agents that are $businessType" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = getRows(tp)

          assertRowMatches(
            agentPremisesRowIndex,
            "tradingpremises.agent.premises.title",
            booleanToLabel(true),
            controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(index, true).url,
            "tradingpremisessummarywho-edit"
          )
        }
      }
    }

    "Business Structure row is rendered" must {

      answersList.foreach { tp =>
        val structure = tp.businessStructure.getOrElse(fail("Business Structure not present"))

        s"show the correct value for ${structure.toString}" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = getRows(tp)

          assertRowMatches(
            1,
            "tradingpremises.businessStructure.cya",
            messages(structure.message),
            controllers.tradingpremises.routes.BusinessStructureController.get(index, true).url,
            "tradingpremisesbusinessstructure-edit"
          )
        }
      }
    }

    "Agent Name row is rendered" must {

      s"render when business type is ${SoleProprietor.toString}" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows(agentSoleProprietor)

        assertRowMatches(
          2,
          "tradingpremises.agentname.name.title",
          agentName,
          controllers.tradingpremises.routes.AgentNameController.get(index, true).url,
          "tradingpremisesagentnametitle-edit"
        )
      }
    }

    "Agent Date of Birth row is rendered" must {

      s"render when business type is ${SoleProprietor.toString}" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows(agentSoleProprietor)

        assertRowMatches(
          3,
          "tradingpremises.agentname.dob.title",
          DateHelper.formatDate(agentDob),
          controllers.tradingpremises.routes.AgentNameController.get(index, true).url,
          "tradingpremisesagentdobtitle-edit"
        )
      }
    }

    "Agent Partnership row is rendered" must {

      s"render when business type is ${Partnership.toString}" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows(agentPartnership)

        assertRowMatches(
          2,
          "tradingpremises.agentpartnership.cya",
          agentPartnershipName,
          controllers.tradingpremises.routes.AgentPartnershipController.get(index, true).url,
          "tradingpremisesagentpartnershiptitle-edit"
        )
      }
    }

    "Agent Company Name row is rendered" must {

      Seq(agentIncorporatedBody, agentLimitedLiabilityPartnership) foreach { tp =>
        val structure = tp.businessStructure.getOrElse(fail("Business Structure not present"))

        s"render when business type is ${structure.toString}" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = getRows(tp)

          assertRowMatches(
            2,
            "tradingpremises.youragent.company.name",
            agentCompanyName,
            controllers.tradingpremises.routes.AgentCompanyDetailsController.get(index, true).url,
            "tradingpremisesagentcompanyname-edit"
          )
        }
      }
    }

    "Agent Company Registration Number row is rendered" must {

      s"render when business type is ${IncorporatedBody.toString}" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows(agentIncorporatedBody)

        assertRowMatches(
          3,
          "tradingpremises.youragent.crn",
          agentCompanyRegNumber,
          controllers.tradingpremises.routes.AgentCompanyDetailsController.get(index, true).url,
          "tradingpremisesyouragentcrn-edit"
        )
      }
    }

    "Trading Premises Name and Address row is rendered" must {

      answersList foreach { tp =>
        val structure = tp.businessStructure.getOrElse(fail("Business Structure not present"))

        s"render when business type is ${structure.toString}" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = getRows(tp)

          val nameAddressIndex: Int = structure match {
            case SoleProprietor | LimitedLiabilityPartnership | IncorporatedBody => 4
            case Partnership                                                     => 3
            case UnincorporatedBody                                              => 2
          }

          val tpName    = tp.yourTradingPremises.map(_.tradingName).getOrElse(fail("Cannot get Trading Name"))
          val tpAddress =
            tp.yourTradingPremises.map(_.tradingPremisesAddress).getOrElse(fail("Cannot get Trading Address"))

          assertRowMatches(
            nameAddressIndex,
            "tradingpremises.yourtradingpremises.title",
            nameAndAddressToLines(tpName, tpAddress),
            controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(index, true).url,
            "tradingpremisesdetails-edit"
          )
        }
      }
    }

    "Trading Premises Start Date row is rendered" must {

      answersList foreach { tp =>
        val structure = tp.businessStructure.getOrElse(fail("Business Structure not present"))

        s"render when business type is ${structure.toString}" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = getRows(tp)

          val dateIndex: Int = structure match {
            case SoleProprietor | LimitedLiabilityPartnership | IncorporatedBody => 5
            case Partnership                                                     => 4
            case UnincorporatedBody                                              => 3
          }

          val tpDate = tp.yourTradingPremises.flatMap(_.startDate).getOrElse(fail("Cannot get Trading Start Date"))

          assertRowMatches(
            dateIndex,
            "tradingpremises.startDate.cya",
            DateHelper.formatDate(tpDate),
            controllers.tradingpremises.routes.ActivityStartDateController.get(index, true).url,
            "tradingprmisedsummarystartdate-edit"
          )
        }
      }
    }

    "Is Residential row is rendered" must {

      answersList foreach { tp =>
        val structure = tp.businessStructure.getOrElse(fail("Business Structure not present"))

        s"render when business type is ${structure.toString}" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = getRows(tp)

          val dateIndex: Int = structure match {
            case SoleProprietor | LimitedLiabilityPartnership | IncorporatedBody => 6
            case Partnership                                                     => 5
            case UnincorporatedBody                                              => 4
          }

          val tpIsResidential =
            tp.yourTradingPremises.flatMap(_.isResidential).getOrElse(fail("Cannot get Is Residential"))

          assertRowMatches(
            dateIndex,
            "tradingpremises.isResidential.title",
            booleanToLabel(tpIsResidential),
            controllers.tradingpremises.routes.IsResidentialController.get(index, true).url,
            "tradingpremisessummaryresidential-edit"
          )
        }
      }
    }

    "Services row is rendered" must {

      answersList foreach { tp =>
        val structure = tp.businessStructure.getOrElse(fail("Business Structure not present"))

        s"render with multiple services when business type is ${structure.toString}" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = getRows(tp)

          val servicesIndex: Int = structure match {
            case SoleProprietor | LimitedLiabilityPartnership | IncorporatedBody => 7
            case Partnership                                                     => 6
            case UnincorporatedBody                                              => 5
          }

          val tpServices =
            tp.whatDoesYourBusinessDoAtThisAddress.map(_.activities).getOrElse(fail("Cannot get Services")).toList

          assertRowMatches(
            servicesIndex,
            "tradingpremises.whatdoesyourbusinessdo.cya",
            toBulletList(
              tpServices.sortBy(_.toString).map(_.value),
              "businessmatching.registerservices.servicename.lbl"
            ),
            controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(index, true).url,
            "tradingpremisedsummaryservices-edit"
          )
        }

        s"render with a single service when business type is ${structure.toString}" in new RowFixture {

          val ba = singleBusinessTypeGen.sample.getOrElse(fail("Cannot generate business activity"))

          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper
              .createSummaryList(
                tp.copy(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(ba)))),
                index,
                true,
                true,
                false
              )
              .rows

          val servicesIndex: Int = structure match {
            case SoleProprietor | LimitedLiabilityPartnership | IncorporatedBody => 7
            case Partnership                                                     => 6
            case UnincorporatedBody                                              => 5
          }

          assertRowMatchesNoChangeLink(
            servicesIndex,
            "tradingpremises.whatdoesyourbusinessdo.cya",
            messages(s"businessmatching.registerservices.servicename.lbl.${ba.value}")
          )
        }
      }
    }

    "MSB Services row is rendered" must {

      answersList foreach { tp =>
        val structure = tp.businessStructure.getOrElse(fail("Business Structure not present"))

        s"render with multiple MSB services when business type is ${structure.toString}" in new RowFixture {
          override val summaryListRows: Seq[SummaryListRow] = getRows(tp)

          val msbIndex: Int = structure match {
            case SoleProprietor | LimitedLiabilityPartnership | IncorporatedBody => 8
            case Partnership                                                     => 7
            case UnincorporatedBody                                              => 6
          }

          val tpServices = tp.msbServices.map(_.services).getOrElse(fail("Cannot get Services")).toList

          assertRowMatches(
            msbIndex,
            "tradingpremises.msb.services.cya",
            toBulletList(tpServices.sortBy(_.toString).map(_.value), "msb.services.list.lbl"),
            controllers.tradingpremises.routes.MSBServicesController.get(index, true).url,
            "tradingpremisesmsbservices-edit"
          )
        }

        s"render with a single service when business type is ${structure.toString}" in new RowFixture {

          val msb = tpSubSectorGen.sample.flatMap(_.services.headOption).getOrElse(fail("Cannot generate msb"))

          override val summaryListRows: Seq[SummaryListRow] =
            cyaHelper
              .createSummaryList(
                tp.copy(msbServices = Some(TradingPremisesMsbServices(Set(msb)))),
                index,
                true,
                false,
                true
              )
              .rows

          val msbIndex: Int = structure match {
            case SoleProprietor | LimitedLiabilityPartnership | IncorporatedBody => 8
            case Partnership                                                     => 7
            case UnincorporatedBody                                              => 6
          }

          assertRowMatchesNoChangeLink(
            msbIndex,
            "tradingpremises.msb.services.cya",
            messages(s"msb.services.list.lbl.${msb.value}")
          )
        }
      }
    }
  }
}
