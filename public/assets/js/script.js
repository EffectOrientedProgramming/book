	/* ========================================================================= */
	/*	Page Preloader
	/* ========================================================================= */

	$(window).on("load", function () {
		$('#preloader').fadeOut('slow', function () {
			$(this).remove();
		});
	});

	(function ($) {
		"use strict";

		/* ========================================================================= */
		/*	Portfolio Filtering Hook
		/* =========================================================================  */
		$('.play-icon i').click(function () {
			var video = '<iframe allowfullscreen src="' + $(this).attr('data-video') + '"></iframe>';
			$(this).replaceWith(video);
		});

		/* ========================================================================= */
		/*	Portfolio Filtering Hook
		/* =========================================================================  */
		setTimeout(function () {
			var filterizd = $('.filtr-container').filterizr({});
			//Active changer
			$('.filtr-control').on('click', function () {
				$('.filtr-control').removeClass("active");
				$(this).addClass("active");
			});
		}, 500);

		/* ========================================================================= */
		/*	Testimonial Carousel
		/* =========================================================================  */

		//Init the slider
		$('.testimonial-slider').slick({
			slidesToShow: 2,
			slidesToScroll: 1,
			infinite: true,
			arrows: false,
			autoplay: true,
			autoplaySpeed: 2000,
			responsive: [{
					breakpoint: 600,
					settings: {
						slidesToShow: 1,
						slidesToScroll: 2
					}
				},
				{
					breakpoint: 480,
					settings: {
						slidesToShow: 1,
						slidesToScroll: 1
					}
				}
			]
		});


		/* ========================================================================= */
		/*	Clients Slider Carousel
		/* =========================================================================  */

		//Init the slider
		$('.clients-logo-slider').slick({
			infinite: true,
			arrows: false,
			autoplay: true,
			autoplaySpeed: 2000,
			slidesToShow: 5,
			slidesToScroll: 1,
		});




		/* ========================================================================= */
		/*	Company Slider Carousel
		/* =========================================================================  */
		$('.company-gallery').slick({
			infinite: true,
			arrows: false,
			autoplay: true,
			autoplaySpeed: 2000,
			slidesToShow: 5,
			slidesToScroll: 1,
		});


		/* ========================================================================= */
		/*	Awars Counter Js
		/* =========================================================================  */
		$('.counter').each(function () {
			var $this = $(this),
				countTo = $this.attr('data-count');

			$({
				countNum: $this.text()
			}).animate({
					countNum: countTo
				},

				{
					duration: 1500,
					easing: 'linear',
					step: function () {
						$this.text(Math.floor(this.countNum));
					},
					complete: function () {
						$this.text(this.countNum);
						//alert('finished');
					}

				});
		});




		/* ========================================================================= */
		/*   Contact Form Validating
		/* ========================================================================= */


		$('#contact-submit').click(function (e) {

			//stop the form from being submitted
			e.preventDefault();

			/* declare the variables, var error is the variable that we use on the end
			to determine if there was an error or not */
			var error = false;
			var name = $('#name').val();
			var email = $('#email').val();
			var subject = $('#subject').val();
			var message = $('#message').val();

			/* in the next section we do the checking by using VARIABLE.length
			where VARIABLE is the variable we are checking (like name, email),
			length is a JavaScript function to get the number of characters.
			And as you can see if the num of characters is 0 we set the error
			variable to true and show the name_error div with the fadeIn effect. 
			if it's not 0 then we fadeOut the div( that's if the div is shown and
			the error is fixed it fadesOut. 
			
			The only difference from these checks is the email checking, we have
			email.indexOf('@') which checks if there is @ in the email input field.
			This JavaScript function will return -1 if no occurrence have been found.*/
			if (name.length == 0) {
				var error = true;
				$('#name').css("border-color", "#D8000C");
			} else {
				$('#name').css("border-color", "#666");
			}
			if (email.length == 0 || email.indexOf('@') == '-1') {
				var error = true;
				$('#email').css("border-color", "#D8000C");
			} else {
				$('#email').css("border-color", "#666");
			}
			if (subject.length == 0) {
				var error = true;
				$('#subject').css("border-color", "#D8000C");
			} else {
				$('#subject').css("border-color", "#666");
			}
			if (message.length == 0) {
				var error = true;
				$('#message').css("border-color", "#D8000C");
			} else {
				$('#message').css("border-color", "#666");
			}

			//now when the validation is done we check if the error variable is false (no errors)
			if (error == false) {
				//disable the submit button to avoid spamming
				//and change the button text to Sending...
				$('#contact-submit').attr({
					'disabled': 'false',
					'value': 'Sending...'
				});

				/* using the jquery's post(ajax) function and a lifesaver
				function serialize() which gets all the data from the form
				we submit it to send_email.php */
				$.post("sendmail.php", $("#contact-form").serialize(), function (result) {
					//and after the ajax request ends we check the text returned
					if (result == 'sent') {
						//if the mail is sent remove the submit paragraph
						$('#cf-submit').remove();
						//and show the mail success div with fadeIn
						$('#mail-success').fadeIn(500);
					} else {
						//show the mail failed div
						$('#mail-fail').fadeIn(500);
						//re enable the submit button by removing attribute disabled and change the text back to Send The Message
						$('#contact-submit').removeAttr('disabled').attr('value', 'Send The Message');
					}
				});
			}
		});


	})(jQuery);



	window.marker = null;

	function initialize() {
		var map;

		var latitude = $('#map').data('lat');
		var longitude = $('#map').data('long');
		var nottingham = new google.maps.LatLng(latitude, longitude);

		var style = [{
			"stylers": [{
				"hue": "#ff61a6"
			}, {
				"visibility": "on"
			}, {
				"invert_lightness": true
			}, {
				"saturation": 40
			}, {
				"lightness": 10
			}]
		}];

		var mapOptions = {
			// SET THE CENTER
			center: nottingham,

			// SET THE MAP STYLE & ZOOM LEVEL
			mapTypeId: google.maps.MapTypeId.ROADMAP,
			zoom: 9,

			// SET THE BACKGROUND COLOUR
			backgroundColor: "#000",

			// REMOVE ALL THE CONTROLS EXCEPT ZOOM
			zoom: 17,
			panControl: false,
			zoomControl: true,
			mapTypeControl: false,
			scaleControl: false,
			streetViewControl: false,
			overviewMapControl: false,
			zoomControlOptions: {
				style: google.maps.ZoomControlStyle.LARGE
			}

		}
		map = new google.maps.Map(document.getElementById('map'), mapOptions);

		// SET THE MAP TYPE
		var mapType = new google.maps.StyledMapType(style, {
			name: "Grayscale"
		});
		map.mapTypes.set('grey', mapType);
		map.setMapTypeId('grey');

		//CREATE A CUSTOM PIN ICON
		var marker_image = $('#map').data('marker');
		var pinIcon = new google.maps.MarkerImage(marker_image, null, null, null, new google.maps.Size(25, 33));

		marker = new google.maps.Marker({
			position: nottingham,
			map: map,
			icon: pinIcon,
			title: 'navigator'
		});
	}

	var map = $('#map');
	if (map.length != 0) {
		google.maps.event.addDomListener(window, 'load', initialize);
	}