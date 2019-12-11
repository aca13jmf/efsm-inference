for i in $(seq 1 $1);
do
	for j in $(seq 1 $1);
	do
		(for k in $(seq 1 $1);
		do
			dotdir="dotfiles-"$i"-"$j"-"$k
			mkdir "dotfiles/"$dotdir
			((i=i%$1)); ((i++==0)) && wait
			java -jar target/scala-2.12/inference-tool-assembly-0.1.0-SNAPSHOT.jar -h distinguish --skip -p $i -u $j -g $k --dotfiles "dotfiles/"$dotdir sample-traces/liftDoors2.json &
		done)
	done
done
make dot
# shutdown
