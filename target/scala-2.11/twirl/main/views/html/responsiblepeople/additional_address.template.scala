
package views.html.responsiblepeople

import play.twirl.api._
import play.twirl.api.TemplateMagic._


     object additional_address_Scope0 {
import models._
import controllers._
import play.api.i18n._
import views.html._
import play.api.templates.PlayMagic._
import play.api.mvc._
import play.api.data._

     object additional_address_Scope1 {
import forms.Form2
import include._
import include.forms2._
import models.autocomplete._

class additional_address extends BaseScalaTemplate[play.twirl.api.HtmlFormat.Appendable,Format[play.twirl.api.HtmlFormat.Appendable]](play.twirl.api.HtmlFormat) with play.twirl.api.Template8[Form2[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
},Boolean,Int,Option[String],String,Option[Seq[NameValuePair]],Request[_$2] forSome { 
   type _$2 >: _root_.scala.Nothing <: _root_.scala.Any
},Messages,play.twirl.api.HtmlFormat.Appendable] {

  /**/
  def apply/*22.2*/(f: Form2[_], edit: Boolean, index: Int, flow: Option[String] = None, personName: String, countryData: Option[Seq[NameValuePair]])(implicit request: Request[_],m:Messages):play.twirl.api.HtmlFormat.Appendable = {
    _display_ {
      {

def /*24.2*/header/*24.8*/:play.twirl.api.HtmlFormat.Appendable = {_display_(

Seq[Any](format.raw/*24.12*/("""
    """),_display_(/*25.6*/components/*25.16*/.back_link()),format.raw/*25.28*/("""
    """),_display_(/*26.6*/errorSummaryWithPlaceholder(f, s"$personName's ${Messages("error.required.select.non.uk.previous.address")}")),format.raw/*26.115*/("""
    """),_display_(/*27.6*/headingWithPlaceholder(("responsiblepeople.additional_address.heading", personName), "summary.responsiblepeople")),format.raw/*27.119*/("""
""")))};
Seq[Any](format.raw/*22.173*/("""

"""),format.raw/*28.2*/("""

"""),_display_(/*30.2*/main(
    title = Messages("responsiblepeople.additional_address.title", personName) + " - " + Messages("summary.responsiblepeople"),
    heading = header
)/*33.2*/ {_display_(Seq[Any](format.raw/*33.4*/("""

    """),_display_(/*35.6*/form(f, controllers.responsiblepeople.routes.AdditionalAddressController.post(index, edit, flow))/*35.103*/ {_display_(Seq[Any](format.raw/*35.105*/("""

        """),format.raw/*37.9*/("""<div class="form-group">
            """),_display_(/*38.14*/fieldset(
                legend = "responsiblepeople.wherepersonlives.is.uk",
                f = f("isUK"),
                panel = false,
                classes = Seq("inline")
            )/*43.14*/ {_display_(Seq[Any](format.raw/*43.16*/("""
                """),_display_(/*44.18*/radio(
                    f = f("isUK"),
                    labelText = "lbl.yes",
                    value = "true",
                    target = s"#${f("postCode").id}-fieldset",
                    inline = true
                )),format.raw/*50.18*/("""
                """),_display_(/*51.18*/radio(
                    f = f("isUK"),
                    labelText = "lbl.no",
                    value = "false",
                    target = s"#${f("country").id}-fieldset",
                    inline = true
                )),format.raw/*57.18*/("""
            """)))}),format.raw/*58.14*/("""
        """),format.raw/*59.9*/("""</div>

        <div class="form-group">
            <div id=""""),_display_(/*62.23*/{s"""${f("postCode").id}-fieldset"""}),format.raw/*62.60*/("""">

                """),_display_(/*64.18*/fieldset(
                    legend = "responsiblepeople.additional_address.address",
                    legendHidden = true,
                    panel = false,
                    f = f(s"""${f("address").id}-fieldset""")
                )/*69.18*/ {_display_(Seq[Any](format.raw/*69.20*/("""
                    """),_display_(/*70.22*/input(f("addressLine1"), labelText = "lbl.address.line1")),format.raw/*70.79*/("""
                    """),_display_(/*71.22*/input(f("addressLine2"), labelText = "lbl.address.line2")),format.raw/*71.79*/("""
                    """),_display_(/*72.22*/input(f("addressLine3"), labelText = "lbl.address.line3")),format.raw/*72.79*/("""
                    """),_display_(/*73.22*/input(f("addressLine4"), labelText = "lbl.address.line4")),format.raw/*73.79*/("""
                    """),_display_(/*74.22*/input(
                        field = f("postCode"),
                        labelText = "responsiblepeople.additional_address.postCode",
                        classes = Seq("postcode")
                    )),format.raw/*78.22*/("""
                """)))}),format.raw/*79.18*/("""
            """),format.raw/*80.13*/("""</div>

            <div id=""""),_display_(/*82.23*/{s"""${f("country").id}-fieldset"""}),format.raw/*82.59*/("""">
                """),_display_(/*83.18*/fieldset(
                    legend = "responsiblepeople.additional_address.address",
                    jsHidden = false,
                    legendHidden = true,
                    panel = false,
                    f = f(s"""${f("address-overseas").id}-fieldset""")
                )/*89.18*/ {_display_(Seq[Any](format.raw/*89.20*/("""
                    """),_display_(/*90.22*/input(f("addressLineNonUK1"), labelText = "lbl.address.line1")),format.raw/*90.84*/("""
                    """),_display_(/*91.22*/input(f("addressLineNonUK2"), labelText = "lbl.address.line2")),format.raw/*91.84*/("""
                    """),_display_(/*92.22*/input(f("addressLineNonUK3"), labelText = "lbl.address.line3")),format.raw/*92.84*/("""
                    """),_display_(/*93.22*/input(f("addressLineNonUK4"), labelText = "lbl.address.line4")),format.raw/*93.84*/("""
                    """),_display_(/*94.22*/country_autocomplete(
                        field = f("country"),
                        placeholder = s"$personName ${Messages("error.required.select.non.uk.previous.address")}",
                        labelText = "responsiblepeople.additional_address.country",
                        data = countryData.getOrElse(Seq.empty)
                    )),format.raw/*99.22*/("""
                """)))}),format.raw/*100.18*/("""
            """),format.raw/*101.13*/("""</div>
        </div>

        """),_display_(/*104.10*/submit(edit, Some("button.saveandcontinue"))),format.raw/*104.54*/("""
    """)))}),format.raw/*105.6*/("""

""")))}),format.raw/*107.2*/("""
"""))
      }
    }
  }

  def render(f:Form2[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
},edit:Boolean,index:Int,flow:Option[String],personName:String,countryData:Option[Seq[NameValuePair]],request:Request[_$2] forSome { 
   type _$2 >: _root_.scala.Nothing <: _root_.scala.Any
},m:Messages): play.twirl.api.HtmlFormat.Appendable = apply(f,edit,index,flow,personName,countryData)(request,m)

  def f:((Form2[_$1] forSome { 
   type _$1 >: _root_.scala.Nothing <: _root_.scala.Any
},Boolean,Int,Option[String],String,Option[Seq[NameValuePair]]) => (Request[_$2] forSome { 
   type _$2 >: _root_.scala.Nothing <: _root_.scala.Any
},Messages) => play.twirl.api.HtmlFormat.Appendable) = (f,edit,index,flow,personName,countryData) => (request,m) => apply(f,edit,index,flow,personName,countryData)(request,m)

  def ref: this.type = this

}


}
}

/**/
object additional_address extends additional_address_Scope0.additional_address_Scope1.additional_address
              /*
                  -- GENERATED --
                  DATE: Mon Jun 24 09:37:30 BST 2019
                  SOURCE: /home/digital/Documents/AMLS/amls-frontend/app/views/responsiblepeople/additional_address_NonUK.scala.htmla.html
                  HASH: 85ffe601522a0efaae1f43f11f6a636de7ed4dda
                  MATRIX: 927->700|1177->874|1191->880|1272->884|1304->890|1323->900|1356->912|1388->918|1519->1027|1551->1033|1686->1146|1729->871|1758->1148|1787->1151|1951->1307|1990->1309|2023->1316|2130->1413|2171->1415|2208->1425|2273->1463|2476->1657|2516->1659|2561->1677|2817->1912|2862->1930|3117->2164|3162->2178|3198->2187|3288->2250|3346->2287|3394->2308|3645->2550|3685->2552|3734->2574|3812->2631|3861->2653|3939->2710|3988->2732|4066->2789|4115->2811|4193->2868|4242->2890|4473->3100|4522->3118|4563->3131|4620->3161|4677->3197|4724->3217|5022->3506|5062->3508|5111->3530|5194->3592|5243->3614|5326->3676|5375->3698|5458->3760|5507->3782|5590->3844|5639->3866|6012->4218|6062->4236|6104->4249|6164->4281|6230->4325|6267->4331|6301->4334
                  LINES: 30->22|34->24|34->24|36->24|37->25|37->25|37->25|38->26|38->26|39->27|39->27|41->22|43->28|45->30|48->33|48->33|50->35|50->35|50->35|52->37|53->38|58->43|58->43|59->44|65->50|66->51|72->57|73->58|74->59|77->62|77->62|79->64|84->69|84->69|85->70|85->70|86->71|86->71|87->72|87->72|88->73|88->73|89->74|93->78|94->79|95->80|97->82|97->82|98->83|104->89|104->89|105->90|105->90|106->91|106->91|107->92|107->92|108->93|108->93|109->94|114->99|115->100|116->101|119->104|119->104|120->105|122->107
                  -- GENERATED --
              */
          