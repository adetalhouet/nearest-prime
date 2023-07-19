bash /home/adetalhouet/skupper/nearest-prime/tools/dbsetup/python/setup-pgsql.sh


oc create ns nearestprime 
oc create ns nearestprime --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig
oc create ns nearestprime --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig


skupper init --namespace nearestprime --site-name ca-central --console-auth=openshift --enable-console --enable-flow-collector
skupper init --namespace nearestprime --site-name ca-regina --console-auth=openshift --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig
skupper init --namespace nearestprime --site-name ca-toronto --console-auth=openshift --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig

skupper token create --namespace nearestprime --token-type cert toronto-token.yaml --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig
skupper token create --namespace nearestprime --token-type cert regina-token.yaml --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig

skupper link create toronto-token.yaml --namespace nearestprime
skupper link create regina-token.yaml --namespace nearestprime

skupper gateway expose db 127.0.0.1 5432 --type podman --namespace nearestprime

oc apply -f /home/adetalhouet/skupper/nearest-prime/yaml/ca-regina-nearestprime.yaml  --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig
oc apply -f /home/adetalhouet/skupper/nearest-prime/yaml/ca-toronto-nearestprime.yaml --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig

skupper expose deployment nearestprime --port 8888 --protocol http --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig
skupper expose deployment nearestprime --port 8888 --protocol http --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig

oc apply -f /home/adetalhouet/skupper/nearest-prime/yaml/load-gen.yaml --namespace nearestprime

loadgen-nearestprime.apps.ca-central.adetalhouet.ca/set_load/0
loadgen-nearestprime.apps.ca-central.adetalhouet.ca/set_load/10


export PGPASSWORD=demopass

watch 'psql --host=localhost --port=5432 --username=demo --dbname=demo-db -c "SELECT * FROM work WHERE nearprime is not null ORDER BY id DESC LIMIT 50;"'

psql --host=localhost --port=5432 --username=demo --dbname=demo-db -c "DELETE FROM WORK WHERE nearprime is not null;"


skupper gateway unforward nearestprime --namespace nearestprime
skupper unexpose deployment nearestprime --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig
skupper unexpose deployment nearestprime --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig
skupper gateway unexpose db --namespace nearestprime

oc delete -f /home/adetalhouet/skupper/nearest-prime/yaml/ca-regina-nearestprime.yaml  --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig
oc delete -f /home/adetalhouet/skupper/nearest-prime/yaml/ca-toronto-nearestprime.yaml --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig
oc delete -f /home/adetalhouet/skupper/nearest-prime/yaml/load-gen.yaml --namespace nearestprime


skupper delete --namespace nearestprime
skupper delete --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig
skupper delete --namespace nearestprime --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig

podman rm -f nearest-prime-db 

oc delete ns nearestprime
oc delete ns nearestprime --kubeconfig /home/adetalhouet/skupper/ca-regina-kubeconfig
oc delete ns nearestprime --kubeconfig /home/adetalhouet/skupper/ca-toronto-kubeconfig
