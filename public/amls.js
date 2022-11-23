/*global $*/

/**
 * PLEASE NOTE
 * If this file is to be modified, please update the version number in the reference to amls.js in main.scala.html and main2.scala.html.
 * This is so that the file can be re-downloaded by clients.
 */

// The follow code moves the focus to the error message summary if there is one, so that the screen reader is alerted.

$(window).load(function () {
        // If there is an error summary, set focus to the summary
        if ($('.amls-error-summary').length) {
            $('.amls-error-summary').focus()
            $('.amls-error-summary a').click(function (e) {
                var href = $(this).attr('href')
                $(href).focus()
            })
        } else {
            // Otherwise, set focus to the field with the error
            $('.error input:first').focus()
        }

        // =====================================================
        // Back link mimics browser back functionality
        // =====================================================
        $('.link-back').on('click', function(e) {
          e.preventDefault();
          history.go(-1); // go(-1) needed for mobile browsers!
        })

        // =====================================================
        // Use GOV.UK shim-links-with-button-role.js to trigger
        // links with role="button" when space key is pressed
        // =====================================================
        GOVUK.shimLinksWithButtonRole.init();
})

$(function () {
  // avoids double panel-indented sections
  // should be CSS but isn't possible.
  $('.panel-indent > .form-field--error')
    .filter(function () {
      return $(this).siblings().not('legend').length === 0;
    })
    .parent().css({
      'padding': 0,
      'border': 'none'
    });

  $.widget('hmrc.auto', {
    _create: function () {
      var options, _select, _change, input, value;
      var _this = this

      function addOption(value) {
        $("<option data-added='true'>")
              .attr("value", value)
              .text(value)
              .prop("selected", true)
              .appendTo(_this.element)
      }

      if (this.element[0].hasAttribute("data-invalid-value")) {
        addOption(this.element.attr("data-invalid-value"))
      }

      value = this.element.find('option:selected').text();

      this.element.hide();

      this.element.parents('form').submit(function (e) {
        _change();
      });

      options = this.element
        .find('option')
        .toArray()
        .map(function (elem) {
          return {
            label: elem.text,
            value: elem.text,
            option: elem
          };
        });

      _select = function (event, ui) {
        ui.item.option.selected = true;
      };

      _change = function (event, ui) {
        options.forEach(function (e) {
          e.option.selected = e.label.toLowerCase() === input.val().toLowerCase();
        });

        if (_this.element.find('option:selected')[0].text == "" && input.val() != "") {
            addOption(input.val())
        }
      };


      input = $('<input>')
          .attr('id', _this.element.attr('id'))
        .insertAfter(this.element)
        .val(value)
        .addClass('form-control')
        .autocomplete({
          source: options,
          select: _select,
          change: _change
        });
        _this.element.attr('id', 'select-' + _this.element.attr('id'))
    }
  });

  var cookieData=GOVUK.getCookie("full-width-banner-cookie");

   if (cookieData == null) {
       $("#full-width-banner").addClass("full-width-banner--show");
   }

   $(".full-width-banner__close").on("click", function(e) {
       e.preventDefault();
       GOVUK.setCookie("full-width-banner-cookie", 1, 99999999999);
       $("#full-width-banner").removeClass("full-width-banner--show");
   });

   var myNav = navigator.userAgent.toLowerCase();
   if(myNav.indexOf('msie 8') == -1)
   {
      $('select[data-auto-complete]').auto();

      $('*[data-add-btn]').click(function () {
          $('select[data-auto-complete]').combobox();
      });
   }

  (function () {
    $.widget('custom.addOne', {
      _create: function () {
        var $this = $(this.element);
        var text = $this.data('add-one');
        var children = $this.children();

        children
          .filter(':not(:first):not(:has(option[selected])):not(.form-field--error)')
          .addClass('js-hidden');
        var $button = $('<a href="#">' + text + '</a>').click(function (e) {
          e.preventDefault();
          $this.find('div.js-hidden:first').fadeIn(500).removeClass('js-hidden');
          if ($this.find('div.js-hidden').size() === 0) {
            $(this).hide();
          }
        });

        if ($this.find('div.js-hidden').size() > 0) {
          $this.append($button.show());
        }
      }
    });
    $('*[data-add-one]').addOne({});
  })();

  (function () {
    var checkedInputs = 'input[type="checkbox"], input[type="radio"]';

    $('input[data-toggle]').each(function () {
      var $self = $(this),
        $target = $($self.data('toggle')),
        $inputs = $target.find('input, option, selected, textarea');

      function pred() {
        var $this = $(this),
          hasValue = false;

        if ($this.is(checkedInputs)) {
          if ($this.prop('checked')) {
            hasValue = true;
          }
        } else if ($this.is('input') && $this.val() !== '') {
          hasValue = true;
        } else if ($this.is('option') && ($this.prop('selected') && $this.val() !== '')) {
          hasValue = true;
        }
        return hasValue;
      }

      if ($target.attr('data-toggle-new')) {
        if ($inputs.filter(pred).length || $self.prop('checked') === true) {
          $target.removeClass('js-hidden');
        }
      } else {
        if ($inputs.filter(pred).length === 0) {
          $self.attr({
            'aria-controls': $target.attr('id'),
            'aria-expanded': 'false'
          });
          if ($self.prop('checked') === false) {
            $target.addClass('js-hidden').attr('aria-hidden', 'true');
          }
        }
      }

      function hide() {
        $inputs.filter(checkedInputs).prop('checked', false);
        $inputs.filter('input, select, textarea').val('');
        $inputs.filter('option').prop('selected', false);
        $self.attr('aria-expanded', 'false');
        $target.addClass('js-hidden').attr('aria-hidden', 'true');
      }

      $self.change(function () {
        if ($self.prop('checked') === true) {
          $self.attr('aria-expanded', 'true');
          $target.removeClass('js-hidden').attr('aria-hidden', 'false');
        } else {
          hide();
        }
      });

      if ($self.prop('type') !== "checkbox") {
        $('input[name="' + $self.prop('name') + '"][value!="' + $self.val() + '"]').change(function () {
          hide();
        });
      }

      if ($self.prop('checked') === true) {
        $target.removeClass('js-hidden');
      }
    });
  }());

  // add keystroke behaviour for anchors with role="button"
  // this is mainly for JAWS screen reader
  $('body').on('keypress','[role="button"]', function(e) {
    if ((e.which === 13) || (e.which === 32)) {
      e.preventDefault();
      this.click();
    }
  });

  // Prevent the user from typing anything other than digits
  $('#wrapper').on('keydown', '[data-digits=true]', function(e) {
      if (!e.shiftKey
          && (/[0-9]/.test(String.fromCharCode(e.keyCode)))       // digits
          || (/[0-9]/.test(String.fromCharCode(e.keyCode - 48)))  // numpad digits
          || e.keyCode == 13                                      // return
          || (e.keyCode >= 37 && e.keyCode < 41)                  // arrow keys
          || e.keyCode == 8                                       // backspace
          || e.keyCode == 9                                       // tab
          || e.keyCode == 46                                      // OSX delete (might be problematic)
          || e.keyCode == 127)                                    // delete
        return true;
      else
        e.preventDefault();
  });

  var duplicateErrorWrapper = $('.duplicate-submission-page');
  var duplicateErrorForm = duplicateErrorWrapper.find('form');

  if (duplicateErrorForm.length) {
    duplicateErrorWrapper.find('.report-error__content.js-hidden').removeClass('js-hidden');
    duplicateErrorWrapper.find('.report-error__toggle').remove();
  }

  var partialDeskproForm = $('.partial-deskpro-form');
  if(partialDeskproForm.length) {

        // activate the on-page ajax link
        // this means the form gets the link-loaded js which sets up validation and submit handler
        $('#get-help-action').click();

        // set up a checker to see when the ajax form gets loaded so we can do some stuff with the form
        var checkForm = setInterval(checkLoaded, 50);

        // see if the form has loaded, once it has, clear the setInterval and do our form stuff
        function checkLoaded(){
            if($('#report-name').length > 0){
                clearInterval(checkForm);
                loadForm();
            }
        }

        // do our form stuff
        function loadForm(){
            // remove the links (as we are actioning them ourselves)
            $('.report-error__toggle').remove();
            // remove the copy which is not required
            $('#report-error-partial-form > h2, #report-error-partial-form > p').remove();

            // pre-populate the action and error fields with our data
            // delete the associated labels (so they don't get picked up by screen-readers)
            // set the inputs to hidden
            if(partialDeskproForm.data('action-value') !== undefined) {
                 var reportActionField = $('#report-action');
                 reportActionField
                     .val(partialDeskproForm.data('action-value'))
                     .attr('type','hidden');
                 $('label[for="' + reportActionField.attr('id') + '"]').remove();
             }

             if(partialDeskproForm.data('error-value') !== undefined) {
                  var reportErrorField = $('#report-error');
                  reportErrorField
                      .val(partialDeskproForm.data('error-value'))
                      .attr('type','hidden');
                  $('label[for="' + reportErrorField.attr('id') + '"]').remove();
              }
        }
  }

  $('[data-gov-autocomplete]').each(function() {
    openregisterLocationPicker({
      additionalEntries: [
         { name: 'European Union', code: 'country:EU' },
         { name: 'Netherlands Antilles', code: 'country:AN' },
         { name: 'Neutral Zone', code: 'country:NT' },
         { name: 'United Nations', code: 'country:UN' },
         { name: 'United States Minor Outlying Islands', code: 'country:UM' },
         { name: 'Saint Helena, Ascension and Tristan da Cunha', code: 'SH' }
      ],
      additionalSynonyms: [
         { name: 'The Ivory Coast', code: 'country:CI' },
         { name: 'Cote d Ivoire', code: 'country:CI' },
         { name: 'Cote dIvoire', code: 'country:CI' },
         { name: 'South Korea', code: 'country:KR' },
         { name: 'North Korea', code: 'country:KP' },
         { name: 'Czech Republic', code: 'country:CZ'},
         { name: 'East Timor', code: 'country:TL'},
         { name: 'Cape Verde', code: 'country:CV'},
         { name: 'Laos', code: 'country:LA'},
         { name: 'St Vincent', code: 'country:VC'},
         { name: 'Vietnam', code: 'country:VN'},
         { name: 'Congo (Democratic Republic)', code: 'country:CD'},
         { name: 'The Gambia', code: 'country:GM'},
         { name: 'Aland Islands', code: 'territory:AX'},
         { name: 'Curacao', code: 'territory:CW'},
         { name: 'Timor Leste', code: 'country:TL'},
         { name: 'Burma', code: 'country:MM'},
         { name: 'Reunion', code: "territory:RE"},
         { name: 'St Helena, Ascension and Tristan da Cunha', code: "territory:SH"},
         { name: 'Ascension Island', code: "territory:SH"},
         { name: 'St Kitts and Nevis', code: "country:KN"},
         { name: 'St Lucia', code: "country:LC"},
         { name: 'St Martin (French part)', code: "territory:MF"},
         { name: 'St Pierre and Miquelon', code: "territory:PM"},
         { name: 'St Vincent and the Grenadines', code: "country:VC"},
         { name: 'Vatican City', code: "country:VA"},
         { name: 'USA', code: "country:US"}
      ],
       defaultValue: '',
      selectElement: this,
      url: '/anti-money-laundering/assets/countries'
    })

    var autocompleteId = this.id
      .replace('-select', '')
      .replace('[', '\\[')
      .replace(']', '\\]');

    function resetSelectIfEmpty(e) {
      var inputIdEscaped = e.target.id
        .replace('[', '\\[')
        .replace(']', '\\]');
      if (inputIdEscaped === autocompleteId) {
        var val = e.target.value.trim();
        var countriesArray = Array.prototype.slice.call(this.options);
        var matches = countriesArray.filter(function (o) {
          return o.text !== '' && o.text === val
        });
        if (!matches.length) {
          this.value = ''
        }
      }
    }

    var wrapper = document.querySelector('#' + autocompleteId + '-wrapper')
    wrapper.addEventListener('change', resetSelectIfEmpty.bind(this))

  })

    $('[data-gov-currency-autocomplete]').each(function() {
        var selectFieldName = $(this).attr('id').replace('[', '\\[').replace(']', '\\]');
        var nonSelectFieldName = selectFieldName.replace('-select','');

        var selectField = $('#' + selectFieldName)
        var nonSelectField = $('#' + nonSelectFieldName)

        nonSelectField.keydown(function(e) {
            if (e.keyCode === 13 && $(this).val() === '') {
                selectField.val('')
            }
        }).keyup(function() {
            var menu = $('.autocomplete__menu')
            if (menu.text() === 'No results found') {
                selectField.val('')
            }
        }).attr('name', nonSelectFieldName + '-autocomp');

        $('body')
            .on('mouseup', ".autocomplete__option > strong", function(e){
                e.preventDefault();
                $(this).parent().trigger('click')
            }).on('click', '.autocomplete__option', function(evt) {
            evt.preventDefault()
            var e = jQuery.Event('keydown');
            e.keyCode = 13;
            $(this).closest('.autocomplete__wrapper').trigger(e);
        })

        $("button[name='submit']").click(function(){

            var selectedOption = $('#' + selectFieldName + ' option:selected')

            if(nonSelectField.val() === '')
                selectField.val('');

            if (selectField.val() === "" && nonSelectField.val() !== "" || selectedOption.text() !== nonSelectField.val())
                addOption(nonSelectField.val())

            function addOption(value) {
                $("<option data-added='true'>")
                    .attr("value", value)
                    .text(value)
                    .prop("selected", true)
                    .appendTo($('#' + selectFieldName))
            }
        })
    })
});
