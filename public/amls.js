/*global $*/
// TODO: Tidy up
$(function () {
    // avoids double panel-indented sections
    // should be CSS but isn't possible.
    $('.panel-indent > .form-field--error').parent().css({
        'padding' : 0,
        'border' : 'none'
    });

    // show/hide
    (function () {
     var checkedInputs = 'input[type="checkbox"], input[type="radio"]';

      function showHide($target, value) {
      console.log(""+$($target))
          var $self = $(this),
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

          $('input[name="' + $self.prop('name') + '"][value!="' + value + '"]').change(function() {
            hide();
          });
      }

      $('*[data-toggle-true]').each(function () {
        var $target = $($(this).data('toggle-true'));
        showHide.call(this, $target, true);
      });

      $('*[data-toggle-false]').each(function () {
        var $target = $($(this).data('toggle-false'));
        showHide.call(this, $target, false);
      });

      $('*[data-toggle-01]').each(function () {
        var $target = $($(this).data('toggle-01')),
            value = $(this).val();
        showHide.call(this, $target, value);
      });

      $('*[data-toggle-02]').each(function () {
        var $target = $($(this).data('toggle-02')),
            value = $(this).val();
        showHide.call(this, $target, value);
      });

      $('*[data-toggle-other]').each(function () {
        var $target = $($(this).data('toggle-other')),
            value = $(this).val();
        showHide.call(this, $target, value);
      });
  })();
});
