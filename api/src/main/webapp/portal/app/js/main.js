/**
 * Main Controller : frame & menu
 * @name MainCtrl
 */

ortolangDiffControllers.controller('MainCtrl', ['$scope', '$http', 'Profil', 'Workspaces', '$rootScope',
    function ($scope, $http, Profil, Workspaces, $rootScope) {

        /* Load profile */
        //Todo: remove hard-code
        var userLogin = 'root';
        var password = 'tagada54';

//        var userLogin = 'user1';
//        var password = 'tagada';

        var auth = btoa(userLogin + ':' + password) ;

        $http.defaults.headers.common.Authorization = 'Basic ' + auth;
        $scope.isLogged = false;
        $scope.profil = Profil.get(
            {userId:userLogin},
            function(profil) {
                $scope.isLogged = true;
            },
            function(error) {
                console.log('something went wrong with the Login');
            }
        );
//        console.log($scope.profil);


        /* Load workspaces */
        $http.defaults.headers.common.Authorization = 'Basic ' + auth;
        $scope.wkList = [];
        $scope.wk = Workspaces.get(
            function(wk) {
                $scope.wkList = wk.entries;
            },
            function(error) {
                console.log('something went wrong while retrieving the list of user\'s workspaces');
            }
        );
//        console.log($scope.wkList);

        $scope.formUser = '';

        $rootScope.$on("Update", function(event, message){
            $scope.formUser = message;
        });

        console.log($scope.formUser);
    }
]);
