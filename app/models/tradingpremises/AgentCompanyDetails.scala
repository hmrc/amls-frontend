/*
 * Copyright 2023 HM Revenue & Customs
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

package models.tradingpremises

import models.FormTypes._
import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import typeclasses.MongoKey

case class AgentCompanyDetails(agentCompanyName: String, companyRegistrationNumber: Option[String])

object AgentCompanyDetails {

  def apply(agentCompanyName: String, companyRegistrationNumber: String): AgentCompanyDetails =
    new AgentCompanyDetails(agentCompanyName, Some(companyRegistrationNumber))

  import utils.MappingUtils.Implicits._

  val maxAgentRegisteredCompanyNameLength = 140
  val maxAgentRegisteredCompanyNumberLength = 8
  val agentsCompanyDetailsType: Rule[String, String] =
    notEmptyStrip andThen notEmpty.withMessage("error.required.tp.agent.company.details") andThen
      maxLength(maxAgentRegisteredCompanyNameLength).withMessage("error.invalid.tp.agent.company.details") andThen
      regexWithMsg(basicPunctuationRegex, "error.invalid.char.tp.agent.company.details")

  val agentsRegisteredCompanyCRNType: Rule[String, String] =
    notEmpty.withMessage("error.required.to.agent.company.reg.number") andThen
      maxLength(maxAgentRegisteredCompanyNumberLength).withMessage("error.size.to.agent.company.reg.number") andThen
      minLength(maxAgentRegisteredCompanyNumberLength).withMessage("error.size.to.agent.company.reg.number") andThen
      regexWithMsg(crnNumberRegex, "error.char.to.agent.company.reg.number")

  implicit val mongoKey = new MongoKey[AgentCompanyDetails] {
    override def apply(): String = "agent-company-name"
  }

  implicit val formReads: Rule[UrlFormEncoded, AgentCompanyDetails] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (
      (__ \ "agentCompanyName").read(agentsCompanyDetailsType) ~
        (__ \ "companyRegistrationNumber").read(agentsRegisteredCompanyCRNType)
      ) (AgentCompanyDetails(_, _))
  }

  implicit val formWrites: Write[AgentCompanyDetails, UrlFormEncoded] = Write {
    case AgentCompanyDetails(name, Some(crn)) => Map("agentCompanyName" -> Seq(name), "companyRegistrationNumber" -> Seq(crn))
    case AgentCompanyDetails(name, _) => Map("agentCompanyName" -> Seq(name))
  }

  implicit val reads: Reads[AgentCompanyDetails] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "agentCompanyName").read[String] and
        (__ \ "companyRegistrationNumber").readNullable[String]
      )(AgentCompanyDetails.apply(_, _))
  }

  implicit val writes: Writes[AgentCompanyDetails] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "agentCompanyName").write[String] and
        (__ \ "companyRegistrationNumber").writeNullable[String]
      )(unlift(AgentCompanyDetails.unapply))
  }

  implicit def conv(name: AgentCompanyName): AgentCompanyDetails ={
    AgentCompanyDetails(name.agentCompanyName, None)
  }

}
