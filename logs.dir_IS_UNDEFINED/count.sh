echo "porovider small: $(cat service-consumer.log| grep 20880 |wc -l)"
echo "porovider medium: $(cat service-consumer.log| grep 20870 |wc -l)"
echo "porovider large: $(cat service-consumer.log| grep 20890 |wc -l)"
