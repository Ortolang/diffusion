/**
 * Login Controller
 * @name loginCtrl
 */

ortolangDiffControllers.controller('loginCtrl', ['$scope', '$http','$rootScope',
    function formController($scope, $http, $rootScope) {

        // create a blank object to hold our form information
        // $scope will allow this to pass between controller and view
        $scope.formUser = {};

        // process the form
//        $scope.processForm = function processForm() {
//            $http({
//                method  : 'POST',
//                url     : 'index.html',
//                data    : $.param($scope.formUser),  // pass in data as strings
//                headers : { 'Content-Type': 'application/x-www-form-urlencoded' }  // set the headers so angular passing info as form data (not request payload)
//            })
//                .success(function(data) {
//                    //console.log(data);
//
//                    if (!data.success) {
//                        // if not successful, bind errors to error variables
//                        $scope.errorName = data.errors.name;
//                        $scope.errorPwd = data.errors.pwd;
//                    } else {
//                        // if successful, bind success message to message
//                        $scope.message = data.message;
//                    }
//                });
//        }

        $scope.processForm = function(formUser) {
            $scope.formUser = angular.copy(formUser);
            $rootScope.$broadcast("Update", "bli");
        };

        $scope.isUnchanged = function(formUser) {
            return angular.equals(formUser, $scope.formUser);
        };


    }

]);
/**
 * Created by cmoro on 18/09/14.
 */
