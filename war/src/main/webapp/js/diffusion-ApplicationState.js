var ApplicationState = function ApplicationState() {

    this.authentified = false;
    this.username = 'guest';
    this.password = '';

    this.login = function (username, password) {
	    this.authentified = true;
	    this.username = username;
	    this.password = password;
    },

    this.logout = function () {
	    this.authentified = false;
	    this.username = 'guest';
	    this.password = '';
    },
	
    this.isAuthentified = function () {
	    return this.authentified;
    },
	
    this.getUsername = function () {
	    return this.username;
    },

    this.getBasicAuthorizationHeader = function () {
	    var base64 = window.btoa(this.username + ":" + this.password);
	    return base64;
    },

    this.save = function () {
	    localStorage.state = JSON.stringify(this);
    }

    this.load = function load() {
        if (localStorage.state) {
            var values = JSON.parse(localStorage.state);
            this.authentified = values['authentified'];
            this.username = values['username'];
            this.password = values['password'];
        }
    }
};

