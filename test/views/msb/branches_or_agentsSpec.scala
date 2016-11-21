package views.msb

import forms.Form2
import models.Country
import models.moneyservicebusiness.BranchesOrAgents
import org.jsoup.Jsoup
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.Request
import play.api.test.FakeRequest

class branches_or_agentsSpec extends WordSpec with MustMatchers with OneAppPerSuite {
  "branches_or_agents view" when {
    "The model contains no countries" must {
      "check the no radio button" in {
        implicit val request : Request[_] = FakeRequest()

        val model= BranchesOrAgents(Some(Seq.empty[Country]))
        val view = views.html.msb.branches_or_agents(Form2(model), false)
        val dom = Jsoup.parse(view.body)
        val noRadio = dom.select("input[id=hasCountries-false]")
        println(s"############# -- $noRadio")
        noRadio.hasAttr("checked") must be (true)
      }
    }

    "The model contains some countries" must {
      "check the yes radio button" in {
        implicit val request : Request[_] = FakeRequest()

        val model= BranchesOrAgents(Some(Seq(Country("COUNTRTY NAME", "CODE"))))
        val form = Form2(model)
        println("=====================================")
        println(s"form = $form")
        println("=====================================")
        val view = views.html.msb.branches_or_agents(form, false)
        val dom = Jsoup.parse(view.body)
        val yesRadio = dom.select("input[id=hasCountries-true]")
        println(s"@@@@@@@@@@@@ --- $yesRadio")
        yesRadio.hasAttr("checked") must be (true)
      }
    }
  }
}
