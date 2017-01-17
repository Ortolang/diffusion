update 
  authorisationpolicy
set
  rulescontent = E'#Fri Jan 13 16:17:52 CET 2017
anonymous=read
esr=read,download
$\{workspace.privileged\}=read,download
'
from
  authorisationpolicy AS ap INNER JOIN authorisationpolicytemplate AS apt ON ap.id = apt.template
where 
  name = 'esr'