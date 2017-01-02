<!DOCTYPE html>
<html>
<head>
    <title></title>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <style type="text/css">
        /* FONTS */
        @media screen {
            @font-face {
                font-family: 'Lato';
                font-style: normal;
                font-weight: 400;
                src: local('Lato Regular'), local('Lato-Regular'), url(https://fonts.gstatic.com/s/lato/v11/qIIYRU-oROkIk8vfvxw6QvesZW2xOQ-xsNqO47m55DA.woff) format('woff');
            }

            @font-face {
                font-family: 'Lato';
                font-style: normal;
                font-weight: 700;
                src: local('Lato Bold'), local('Lato-Bold'), url(https://fonts.gstatic.com/s/lato/v11/qdgUG4U09HnJwhYI-uK18wLUuEpTyoUstqEm5AMlJo4.woff) format('woff');
            }

            @font-face {
                font-family: 'Lato';
                font-style: italic;
                font-weight: 400;
                src: local('Lato Italic'), local('Lato-Italic'), url(https://fonts.gstatic.com/s/lato/v11/RYyZNoeFgb0l7W3Vu1aSWOvvDin1pK8aKteLpeZ5c0A.woff) format('woff');
            }

            @font-face {
                font-family: 'Lato';
                font-style: italic;
                font-weight: 700;
                src: local('Lato Bold Italic'), local('Lato-BoldItalic'), url(https://fonts.gstatic.com/s/lato/v11/HkF_qI1x_noxlxhrhMQYELO3LdcAZYWl9Si6vvxL-qU.woff) format('woff');
            }
        }

        /* CLIENT-SPECIFIC STYLES */
        body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
        table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
        img { -ms-interpolation-mode: bicubic; }

        /* RESET STYLES */
        img { border: 0; height: auto; line-height: 100%; outline: none; text-decoration: none; }
        table { border-collapse: collapse !important; }
        body { height: 100% !important; margin: 0 !important; padding: 0 !important; width: 100% !important; }

        /* iOS BLUE LINKS */
        a[x-apple-data-detectors] {
            color: inherit !important;
            text-decoration: none !important;
            font-size: inherit !important;
            font-family: inherit !important;
            font-weight: inherit !important;
            line-height: inherit !important;
        }

        /* MOBILE STYLES */
        @media screen and (max-width:600px){
            h1 {
                font-size: 32px !important;
                line-height: 32px !important;
            }
        }

        /* ANDROID CENTER FIX */
        div[style*="margin: 16px 0;"] { margin: 0 !important; }

        .blockquote {
            padding: 10px 20px;
            margin: 0 0 20px;
            border-left: 5px solid #eee;
        }
    </style>
</head>
<body style="background-color: #f4f4f4; margin: 0 !important; padding: 0 !important;">

<!-- HIDDEN PREHEADER TEXT -->
<div style="display: none; font-size: 1px; color: #fefefe; line-height: 1px; font-family: 'Lato', Helvetica, Arial, sans-serif; max-height: 0px; max-width: 0px; opacity: 0; overflow: hidden;">${msg(body, args)}</div>

<table border="0" cellpadding="0" cellspacing="0" width="100%">
    <!-- LOGO -->
    <tr>
        <td bgcolor="#008bd0" align="center">
            <!--[if (gte mso 9)|(IE)]>
            <table align="center" border="0" cellspacing="0" cellpadding="0" width="600">
                <tr>
                    <td align="center" valign="top" width="600">
            <![endif]-->
            <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px;" >
                <tr>
                    <td align="center" valign="top" style="padding: 40px 10px 40px 10px;">
                        <a href="${marketUrl}" target="_blank">
                            <img alt="Logo" src="data:image/png;base64,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" width="132" height="70" style="display: block; width: 132px; max-width: 132px; min-width: 132px; font-family: 'Lato', Helvetica, Arial, sans-serif; color: #ffffff; font-size: 18px;" border="0">
                        </a>
                    </td>
                </tr>
            </table>
            <!--[if (gte mso 9)|(IE)]>
            </td>
            </tr>
            </table>
            <![endif]-->
        </td>
    </tr>
    <!-- HERO -->
    <tr>
        <td bgcolor="#008bd0" align="center" style="padding: 0px 10px 0px 10px;">
            <!--[if (gte mso 9)|(IE)]>
            <table align="center" border="0" cellspacing="0" cellpadding="0" width="600">
                <tr>
                    <td align="center" valign="top" width="600">
            <![endif]-->
            <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px;" >
                <tr>
                    <td bgcolor="#ffffff" align="center" style="padding: 30px 30px 0px 30px; border-radius: 4px 4px 0px 0px;  color: #111111; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 18px; font-weight: 400; line-height: 25px;" >
                        <h2 style="font-size: 24px; font-weight: 400; margin: 0;">${msg(title, args)}</h2>
                    </td>
                </tr>
            </table>
            <!--[if (gte mso 9)|(IE)]>
            </td>
            </tr>
            </table>
            <![endif]-->
        </td>
    </tr>
    <!-- COPY BLOCK -->
    <tr>
        <td bgcolor="#f4f4f4" align="center" style="padding: 0px 10px 0px 10px;">
            <!--[if (gte mso 9)|(IE)]>
            <table align="center" border="0" cellspacing="0" cellpadding="0" width="600">
                <tr>
                    <td align="center" valign="top" width="600">
            <![endif]-->
            <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px;" >
                <!-- COPY -->
                <tr>
                    <td bgcolor="#ffffff" align="left" style="padding: 20px 30px 30px 30px; border-radius: 0px 0px 4px 4px; color: #666666; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 18px; font-weight: 400; line-height: 25px;" >${msg(body, args)}</td>
                </tr>
            </table>
            <!--[if (gte mso 9)|(IE)]>
            </td>
            </tr>
            </table>
            <![endif]-->
        </td>
    </tr>
    <!-- SUPPORT CALLOUT -->
    <tr>
        <td bgcolor="#f4f4f4" align="center" style="padding: 30px 10px 0px 10px;">
            <!--[if (gte mso 9)|(IE)]>
            <table align="center" border="0" cellspacing="0" cellpadding="0" width="600">
                <tr>
                    <td align="center" valign="top" width="600">
            <![endif]-->
            <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px;" >
                <!-- HEADLINE -->
                <tr>
                    <td bgcolor="#475b6e" align="center" style="padding: 30px 30px 30px 30px; border-radius: 4px 4px 4px 4px; color: #f4f4f4; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 18px; font-weight: 400; line-height: 25px;" >
                        <h2 style="font-size: 20px; font-weight: 400; color: #f4f4f4; margin: 0;">${msg("help")}</h2>
                        <p style="margin: 0;"><a href="mailto:contact@ortolang.fr" style="color: #f4f4f4;">contact@ortolang.fr</a></p>
                    </td>
                </tr>
            </table>
            <!--[if (gte mso 9)|(IE)]>
            </td>
            </tr>
            </table>
            <![endif]-->
        </td>
    </tr>
    <!-- FOOTER -->
    <tr>
        <td bgcolor="#f4f4f4" align="center" style="padding: 0px 10px 0px 10px;">
            <!--[if (gte mso 9)|(IE)]>
            <table align="center" border="0" cellspacing="0" cellpadding="0" width="600">
                <tr>
                    <td align="center" valign="top" width="600">
            <![endif]-->
            <table border="0" cellpadding="0" cellspacing="0" width="100%" style="max-width: 600px;" >
                <!-- NAVIGATION -->
                <tr>
                    <td bgcolor="#f4f4f4" align="left" style="padding: 30px 30px 30px 30px; color: #666666; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 14px; font-weight: 400; line-height: 18px;" >
                        <p style="margin: 0;">
                            <a href="${marketUrl}" target="_blank" style="color: #111111; font-weight: 700;">${msg("home")}</a> -
                            <#if userType == "moderator"><a href="${marketUrl}profiles/me/tasks" target="_blank" style="color: #111111; font-weight: 700;">${msg("tasks")}</a> -</#if>
                            <a href="${marketUrl}workspaces" target="_blank" style="color: #111111; font-weight: 700;">${msg("workspaces")}</a> -
                            <a href="${marketUrl}profiles/me/edition" target="_blank" style="color: #111111; font-weight: 700;">${msg("profile")}</a> -
                            <a href="${marketUrl}information/presentation" target="_blank" style="color: #111111; font-weight: 700;">${msg("information")}</a>
                        </p>
                    </td>
                </tr>
                <tr>
                    <td bgcolor="#f4f4f4" align="left" style="padding: 0px 30px 30px 30px; color: #666666; font-family: 'Lato', Helvetica, Arial, sans-serif; font-size: 12px; font-weight: 400; line-height: 18px;" >
                        <p style="margin: 0;">${msg("footer")}</p>
                    </td>
                </tr>
            </table>
            <!--[if (gte mso 9)|(IE)]>
            </td>
            </tr>
            </table>
            <![endif]-->
        </td>
    </tr>
</table>

</body>
</html>
