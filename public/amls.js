/*global $*/
// TODO: Tidy up
$(function () {
    // avoids double panel-indented sections
    // should be CSS but isn't possible.
    // TODO: This needs a different approach
    $('.panel-indent > .form-field--error')
        .filter(function () {
            return $(this).siblings().not('legend').length === 0;
        })
        .parent().css({
            'padding' : 0,
            'border' : 'none'
        });

    $.widget( 'custom.combobox', {
      _create: function() {
        this.wrapper = $( '<span>' )
          .addClass( 'custom-combobox' )
          .insertAfter( this.element );

        this.element.hide();
        this._createAutocomplete();
        this._createShowAllButton();
      },

      _createAutocomplete: function() {
        var selected = this.element.children( ':selected' ),
          value = selected.val() ? selected.text() : '';

        this.input = $( '<input>' )
          .appendTo( this.wrapper )
          .val( value )
          .attr( 'title', '' )
          .addClass( 'custom-combobox-input ui-widget ui-widget-content ui-state-default ui-corner-left' )
          .autocomplete({
            delay: 0,
            minLength: 0,
            source: $.proxy( this, '_source' )
          });

        this._on( this.input, {
          autocompleteselect: function( event, ui ) {
            ui.item.option.selected = true;
            this._trigger( 'select', event, {
              item: ui.item.option
            });
          },

          autocompletechange: '_removeIfInvalid'
        });
      },

      _createShowAllButton: function() {
        var input = this.input,
          wasOpen = false;

        $( '<a>' )
          .attr( 'tabIndex', -1 )
          .attr( 'title', 'Show All Items' )
          .appendTo( this.wrapper )
          .removeClass( 'ui-corner-all' )
          .addClass( 'custom-combobox-toggle ui-corner-right' )
          .mousedown(function() {
            wasOpen = input.autocomplete( 'widget' ).is( ':visible' );
          })
          .click(function() {
            input.focus();

            // Close if already visible
            if ( wasOpen ) {
              return;
            }

            // Pass empty string as value to search for, displaying all results
            input.autocomplete( 'search', '' );
          });
      },

      _source: function( request, response ) {
        var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), 'i' );
        response( this.element.children( 'option' ).map(function() {
          var text = $( this ).text();
          if ( this.value && ( !request.term || matcher.test(text) ) )
            return {
              label: text,
              value: text,
              option: this
            };
        }) );
      },

      _removeIfInvalid: function( event, ui ) {

        // Selected an item, nothing to do
        if ( ui.item ) {
          return;
        }

        // Search for a match (case-insensitive)
        var value = this.input.val(),
          valueLowerCase = value.toLowerCase(),
          valid = false;
        this.element.children( 'option' ).each(function() {
          if ( $( this ).text().toLowerCase() === valueLowerCase ) {
            this.selected = valid = true;
            return false;
          }
        });

        // Found a match, nothing to do
        if ( valid ) {
          return;
        }

        // Remove invalid value
        this.input
          .val( '' )
          .attr( 'title', value + ' didn’t match any item' );
        this.element.val( '' );
        this.input.autocomplete( 'instance' ).term = '';
      },

      _destroy: function() {
        this.wrapper.remove();
        this.element.show();
      }
    });

    // $('.country-selector').selectToAutocomplete();

    // $('*[data-add-btn]').click(function () {
    //     $('select.country-selector').selectToAutocomplete();
    // });

    $('select.country-selector').combobox();

    $('*[data-add-btn]').click(function () {
        $('select.country-selector').combobox();
    });

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

            if ($inputs.filter(pred).length === 0) {
                if ($self.prop('checked') === false) {
                    $target.hide();
                }
            }

            function hide() {
                $inputs.filter(checkedInputs).prop('checked', false);
                $inputs.filter('input, select, textarea').val('');
                $inputs.filter('option').prop('selected', false);
                $target.hide();
            }

            $self.change(function () {
                if ($self.prop('checked') === true) {
                    $target.show();
                } else {
                    hide();
                }
            });

            if ($self.prop('type') != "checkbox") {
                $('input[name="' + $self.prop('name') + '"][value!="' + $self.val() + '"]').change(function () {
                    hide();
                });
            }

            if ($self.prop('checked') === true) {
                $target.show();
            }
        });
    }());
});
